#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
订单并发性能与一致性测试脚本
- 并发压测订单创建与支付
- 评估优化前后QPS/TPS与响应时间
- 校验 Redis 库存 与 成功支付订单数 的一致性（无超卖）

使用方式：
  python test/performance/order_concurrency_benchmark.py \
      --base-url http://localhost:8000 \
      --ticket-id 1 \
      --show-id 1 \
      --session-id 1 \
      --initial-stock 1000 \
      --concurrency 100 \
      --requests 1000 \
      --auth-token "Bearer <YOUR_TOKEN>"

说明：
- 默认通过网关 http://localhost:8000 调用 /api/order 接口；如需直连订单服务可改为 http://localhost:8083
- 可选：直接初始化Redis库存（无需依赖上游库存接口）
- 需要安装依赖：requests、redis

pip install -r test/requirements.txt
或
pip install requests redis
"""

import argparse
import json
import os
import random
import string
import time
from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import datetime
from statistics import mean, median

import requests
import redis


def login_get_token(base_url: str, username: str, password: str) -> str:
    """登录获取JWT token，兼容Result包装结构。"""
    url = f"{base_url}/api/user/login"
    payload = {"username": username, "password": password}
    try:
        resp = requests.post(url, json=payload, timeout=5)
        if resp.status_code != 200:
            print(f"[LOGIN] HTTP {resp.status_code}: {resp.text}")
            return ""
        data = {}
        try:
            data = resp.json()
        except Exception:
            print("[LOGIN] 非JSON响应")
            return ""
        token = ""
        if isinstance(data, dict):
            # 优先Result.data.token
            if isinstance(data.get("data"), dict):
                token = data["data"].get("token", "")
            # 或顶层token
            if not token:
                token = data.get("token", "")
        if token:
            print("[LOGIN] 获取token成功")
        else:
            print(f"[LOGIN] 响应无token字段: {data}")
        return token
    except Exception as e:
        print(f"[LOGIN] 异常: {e}")
        return ""


def rand_suffix(n=6):
    return ''.join(random.choices(string.ascii_lowercase + string.digits, k=n))


def init_redis_stock(rds: redis.Redis, ticket_id: int, stock: int, stock_key_prefix: str = "ticket_stock:"):
    key = f"{stock_key_prefix}{ticket_id}"
    rds.set(key, stock)
    return key


def is_ticket_valid(base_url: str, headers: dict, ticket_id: int) -> bool:
    """调用 /api/ticket/price/{ticketId} 校验票档是否存在且价格有效。"""
    url = f"{base_url}/api/ticket/price/{ticket_id}"
    try:
        resp = requests.get(url, headers=headers, timeout=5)
        if resp.status_code != 200:
            return False
        data = {}
        try:
            data = resp.json()
        except Exception:
            return False
        # 兼容 Result 包装: { code, data: number }
        if isinstance(data, dict):
            val = data.get("data")
            # 数值且 > 0 认为有效
            if isinstance(val, (int, float)):
                return val > 0
        return False
    except Exception:
        return False


def get_redis_stock(rds: redis.Redis, ticket_id: int, stock_key_prefix: str = "ticket_stock:"):
    key = f"{stock_key_prefix}{ticket_id}"
    val = rds.get(key)
    return int(val) if val is not None else None


def create_order(base_url: str, headers: dict, payload: dict):
    url = f"{base_url}/api/order/create"
    start = time.time()
    try:
        resp = requests.post(url, json=payload, headers=headers, timeout=5)
        elapsed = (time.time() - start) * 1000
        ok = resp.status_code == 200
        data = None
        try:
            data = resp.json()
        except Exception:
            pass
        # 解析订单号（Result<String> -> data 即为订单号）
        order_no = None
        if isinstance(data, dict):
            if isinstance(data.get("data"), str):
                order_no = data["data"]
            elif "orderNo" in data:
                order_no = data.get("orderNo")
            elif "order_no" in data:
                order_no = data.get("order_no")
        return ok, elapsed, resp.status_code, data, order_no
    except Exception as e:
        elapsed = (time.time() - start) * 1000
        return False, elapsed, 0, {"error": str(e)}, None


def pay_order(base_url: str, headers: dict, order_no: str, pay_type: int = 1):
    url = f"{base_url}/api/order/pay"
    # OrderController.payOrder 使用 @RequestParam，需以查询参数或表单提交
    params = {"orderNo": order_no, "payType": str(pay_type)}
    start = time.time()
    try:
        resp = requests.post(url, params=params, headers=headers, timeout=5)
        elapsed = (time.time() - start) * 1000
        ok = resp.status_code == 200
        data = None
        try:
            data = resp.json()
        except Exception:
            pass
        return ok, elapsed, resp.status_code, data
    except Exception as e:
        elapsed = (time.time() - start) * 1000
        return False, elapsed, 0, {"error": str(e)}


def run_benchmark(args):
    # Redis client
    rds = redis.Redis(host=args.redis_host, port=args.redis_port, db=args.redis_db, password=args.redis_password or None, decode_responses=True)

    # 构建请求头与认证
    headers = {
        "Content-Type": "application/json"
    }
    token = args.auth_token
    if (not token) and args.login_user and args.login_pass:
        t = login_get_token(args.base_url, args.login_user, args.login_pass)
        if t:
            token = "Bearer " + t if not t.startswith("Bearer ") else t
    if token:
        headers["Authorization"] = token

    # 票档有效性校验与自动发现
    if not is_ticket_valid(args.base_url, headers, args.ticket_id):
        if args.auto_discover_ticket:
            print(f"[CHECK] ticketId={args.ticket_id} 无效，尝试自动发现有效票档...")
            found_id = None
            for cand in range(args.ticket_id_start, args.ticket_id_start + args.ticket_id_attempts):
                if is_ticket_valid(args.base_url, headers, cand):
                    found_id = cand
                    break
            if found_id is None:
                print("[CHECK] 未能发现有效票档，请确认演出/票档数据是否准备就绪或指定 --ticket-id")
                return
            print(f"[CHECK] 使用发现的有效 ticketId={found_id}")
            args.ticket_id = found_id
        else:
            print(f"[CHECK] ticketId={args.ticket_id} 无效，终止测试。可开启 --auto-discover-ticket 自动探测。")
            return

    # 初始化库存（在票档校验之后）
    stock_key = init_redis_stock(rds, args.ticket_id, args.initial_stock)
    print(f"[INIT] Redis stock key={stock_key}, value={args.initial_stock}")

    # 构造并发请求参数
    total = args.requests
    users = [args.user_id or (100000 + i) for i in range(args.concurrency)]

    create_results = []
    pay_results = []

    def worker(i):
        # 每个任务创建并支付一张票（可扩展 quantity）
        user_id = users[i % len(users)]
        order_req = {
            "userId": user_id,
            "showId": args.show_id,
            "sessionId": args.session_id,
            "ticketId": args.ticket_id,
            "quantity": 1
        }
        ok, elapsed, code, data, order_no = create_order(args.base_url, headers, order_req)
        # order_no 已在 create_order 中解析，data 为原始响应体
        create_results.append({
            "ok": ok, "elapsed_ms": elapsed, "status": code, "order_no": order_no, "resp": data
        })
        if ok and order_no:
            ok2, elapsed2, code2, data2 = pay_order(args.base_url, headers, order_no, args.pay_type)
            pay_results.append({
                "ok": ok2, "elapsed_ms": elapsed2, "status": code2, "order_no": order_no, "resp": data2
            })
        else:
            pay_results.append({"ok": False, "elapsed_ms": 0, "status": 0, "order_no": order_no, "resp": None})

    print(f"[RUN] Concurrency={args.concurrency}, Total Requests={total}")
    t0 = time.time()
    with ThreadPoolExecutor(max_workers=args.concurrency) as ex:
        futures = [ex.submit(worker, i) for i in range(total)]
        for _ in as_completed(futures):
            pass
    t1 = time.time()

    # 统计
    create_ok = [r for r in create_results if r["ok"]]
    pay_ok = [r for r in pay_results if r["ok"]]
    create_times = [r["elapsed_ms"] for r in create_results]
    pay_times = [r["elapsed_ms"] for r in pay_results if r["elapsed_ms"] > 0]

    duration = t1 - t0
    create_tps = len(create_ok) / duration
    pay_tps = len(pay_ok) / duration

    print("\n===== 性能结果 =====")
    print(f"总耗时: {duration:.2f}s")
    print(f"创建成功数: {len(create_ok)}/{len(create_results)} | TPS: {create_tps:.2f}")
    if create_times:
        print(f"创建耗时: avg={mean(create_times):.2f}ms, p50={median(create_times):.2f}ms, max={max(create_times):.2f}ms")
    print(f"支付成功数: {len(pay_ok)}/{len(pay_results)} | TPS: {pay_tps:.2f}")
    if pay_times:
        print(f"支付耗时: avg={mean(pay_times):.2f}ms, p50={median(pay_times):.2f}ms, max={max(pay_times):.2f}ms")

    # 一致性检查
    remain = get_redis_stock(rds, args.ticket_id)
    sold = len(pay_ok)
    expected_remain = args.initial_stock - sold

    print("\n===== 一致性检查 =====")
    print(f"初始库存: {args.initial_stock}")
    print(f"成功支付订单数(售出): {sold}")
    print(f"Redis剩余库存: {remain}")
    if remain is not None:
        if remain == expected_remain:
            print("一致性OK: Redis库存与支付订单数一致")
        else:
            print(f"一致性异常: 期望剩余 {expected_remain} 与 Redis 实际 {remain} 不一致")
    else:
        print("未能读取Redis库存，请检查Redis连接或key前缀")

    # 保存报告
    report = {
        "timestamp": datetime.now().isoformat(),
        "base_url": args.base_url,
        "concurrency": args.concurrency,
        "total_requests": total,
        "initial_stock": args.initial_stock,
        "sold": sold,
        "redis_remain": remain,
        "expected_remain": expected_remain,
        "duration_sec": duration,
        "create_success": len(create_ok),
        "pay_success": len(pay_ok),
        "create_tps": create_tps,
        "pay_tps": pay_tps,
        "latency_ms": {
            "create_avg": mean(create_times) if create_times else None,
            "create_p50": median(create_times) if create_times else None,
            "create_max": max(create_times) if create_times else None,
            "pay_avg": mean(pay_times) if pay_times else None,
            "pay_p50": median(pay_times) if pay_times else None,
            "pay_max": max(pay_times) if pay_times else None,
        }
    }
    os.makedirs("test/performance", exist_ok=True)
    out = f"test/performance/order_benchmark_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json"
    with open(out, "w", encoding="utf-8") as f:
        json.dump(report, f, ensure_ascii=False, indent=2)
    print(f"\n报告已保存: {out}")


def parse_args():
    p = argparse.ArgumentParser(description="订单并发性能与一致性测试")
    p.add_argument("--base-url", default=os.environ.get("ORDER_BASE_URL", "http://localhost:8000"), help="订单服务网关或直连地址")
    p.add_argument("--ticket-id", type=int, default=int(os.environ.get("TEST_TICKET_ID", "1")))
    p.add_argument("--show-id", type=int, default=int(os.environ.get("TEST_SHOW_ID", "1")))
    p.add_argument("--session-id", type=int, default=int(os.environ.get("TEST_SESSION_ID", "1")))
    p.add_argument("--initial-stock", type=int, default=int(os.environ.get("INITIAL_STOCK", "1000")))
    p.add_argument("--concurrency", type=int, default=int(os.environ.get("CONCURRENCY", "100")))
    p.add_argument("--requests", type=int, default=int(os.environ.get("REQUESTS", "1000")))
    p.add_argument("--pay-type", type=int, default=int(os.environ.get("PAY_TYPE", "1")))
    p.add_argument("--user-id", type=int, default=None, help="固定用户ID（默认随机分布多个用户）")
    p.add_argument("--auth-token", default=os.environ.get("AUTH_TOKEN", ""), help="HTTP Authorization 头，例如: Bearer xxx")
    p.add_argument("--login-user", default=os.environ.get("LOGIN_USER", "admin"))
    p.add_argument("--login-pass", default=os.environ.get("LOGIN_PASS", "123456"))
    # 票档校验与自动发现
    p.add_argument("--auto-discover-ticket", action="store_true", help="若ticketId无效，自动尝试从起始ID向后探测有效票档")
    p.add_argument("--ticket-id-start", type=int, default=int(os.environ.get("TICKET_ID_START", "1")))
    p.add_argument("--ticket-id-attempts", type=int, default=int(os.environ.get("TICKET_ID_ATTEMPTS", "50")))
    # Redis
    p.add_argument("--redis-host", default=os.environ.get("REDIS_HOST", "localhost"))
    p.add_argument("--redis-port", type=int, default=int(os.environ.get("REDIS_PORT", "6379")))
    p.add_argument("--redis-db", type=int, default=int(os.environ.get("REDIS_DB", "0")))
    p.add_argument("--redis-password", default=os.environ.get("REDIS_PASSWORD", ""))
    return p.parse_args()


if __name__ == "__main__":
    args = parse_args()
    print("参数:", args)
    run_benchmark(args)
