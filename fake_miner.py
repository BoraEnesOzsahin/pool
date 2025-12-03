"""TESTNET ONLY. DO NOT USE ON MAINNET."""

import logging
import os
import sys
import time
from decimal import Decimal
from typing import Optional

from web3 import Web3


def get_env_var(name: str, default: Optional[str] = None) -> str:
    value = os.getenv(name, default)
    if value is None or value.strip() == "":
        logging.error("Missing required environment variable: %s", name)
        raise SystemExit(1)
    return value.strip()


def build_web3_client() -> Web3:
    rpc_url = get_env_var("SEPOLIA_HTTP_URL")
    web3 = Web3(Web3.HTTPProvider(rpc_url))
    if not web3.is_connected():
        logging.error("Unable to connect to Sepolia RPC at %s", rpc_url)
        raise SystemExit(1)
    return web3


def main() -> None:
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s [%(levelname)s] %(message)s",
        datefmt="%Y-%m-%d %H:%M:%S",
    )

    web3 = build_web3_client()

    funder_private_key = get_env_var("FUNDER_PRIVATE_KEY")
    hot_wallet = Web3.to_checksum_address(get_env_var("HOT_WALLET_ADDRESS"))
    payout_value_wei = Web3.to_wei(Decimal("0.02"), "ether")

    try:
        payout_interval = int(os.getenv("PAYOUT_INTERVAL_SECONDS", "60"))
    except ValueError:
        logging.warning(
            "Invalid PAYOUT_INTERVAL_SECONDS supplied, defaulting to 60 seconds"
        )
        payout_interval = 60

    account = web3.eth.account.from_key(funder_private_key)
    funder_address = account.address

    logging.info(
        "Starting fake miner payouts from %s to %s every %s seconds",
        funder_address,
        hot_wallet,
        payout_interval,
    )

    while True:
        try:
            nonce = web3.eth.get_transaction_count(funder_address, "pending")
            latest_block = web3.eth.get_block("latest")
            base_fee = latest_block.get("baseFeePerGas", web3.eth.gas_price)
            priority_fee = Web3.to_wei(2, "gwei")

            tx = {
                "chainId": web3.eth.chain_id,
                "nonce": nonce,
                "to": hot_wallet,
                "value": payout_value_wei,
                "gas": 21_000,
                "maxPriorityFeePerGas": priority_fee,
                "maxFeePerGas": base_fee + priority_fee,
            }

            signed_tx = account.sign_transaction(tx)
            tx_hash = web3.eth.send_raw_transaction(signed_tx.raw_transaction)
            logging.info(
                "Sent payout tx %s (nonce %s, %s Sepolia ETH)",
                tx_hash.hex(),
                nonce,
                Web3.from_wei(payout_value_wei, "ether"),
            )
        except Exception as exc:  # noqa: BLE001 - we want to keep loop alive
            logging.exception("Failed to send payout: %s", exc)

        time.sleep(max(payout_interval, 1))


if __name__ == "__main__":
    try:
        main()
    except SystemExit:
        raise
    except Exception:
        logging.exception("Fatal error in fake miner script")
        sys.exit(1)
