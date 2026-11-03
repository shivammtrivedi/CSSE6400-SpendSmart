import json
import os
import uuid
import boto3
import psycopg2

_conn = None


def _get_secret() -> dict:
    client = boto3.client("secretsmanager", region_name=os.environ["AWS_REGION"])
    response = client.get_secret_value(SecretId=os.environ["DB_SECRET_ARN"])
    return json.loads(response["SecretString"])


def _get_connection():
    global _conn
    if _conn is None or _conn.closed:
        secret = _get_secret()
        _conn = psycopg2.connect(
            host=secret["host"],
            port=secret.get("port", 5432),
            dbname=secret["dbname"],
            user=secret["username"],
            password=secret["password"],
            connect_timeout=5,
            sslmode="require",
        )
    return _conn


def handler(event, context):
    """Cognito Post Confirmation trigger — creates user row in the application DB."""
    attrs = event.get("request", {}).get("userAttributes", {})

    raw_user_id = attrs.get("sub")
    email = attrs.get("email")
    name = attrs.get("name") or attrs.get("preferred_username")

    if not raw_user_id or not email:
        # Should never happen for a confirmed Cognito user, but be safe.
        raise ValueError(f"Missing required attributes: sub={raw_user_id}, email={email}")

    try:
        user_id = uuid.UUID(raw_user_id)
    except ValueError as ex:
        raise ValueError(f"Invalid Cognito sub UUID: {raw_user_id}") from ex

    conn = _get_connection()
    try:
        with conn.cursor() as cur:
            # Contract aligned with shared-schema User entity:
            # users(id UUID, email, name, created_at)
            cur.execute(
                """
                INSERT INTO users (id, email, name, created_at)
                VALUES (%s::uuid, %s, %s, now())
                ON CONFLICT (id) DO UPDATE
                  SET email = EXCLUDED.email,
                      name = EXCLUDED.name
                """,
                (str(user_id), email, name),
            )
        conn.commit()
    except Exception:
        conn.rollback()
        raise

    # Must return the event unchanged so Cognito continues the flow.
    return event
