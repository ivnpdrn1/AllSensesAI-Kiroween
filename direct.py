import json
def lambda_handler(event, context):
    return {
        "statusCode": 200,
        "headers": {"Access-Control-Allow-Origin": "*", "Content-Type": "application/json"},
        "body": json.dumps({"status": "success", "message": "AllSenses AI Guardian is LIVE!", "version": "3.0", "timestamp": context.aws_request_id})
    }
