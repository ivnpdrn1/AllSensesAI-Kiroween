import json
def lambda_handler(event, context):
    return {
        "statusCode": 200,
        "headers": {"Access-Control-Allow-Origin": "*"},
        "body": json.dumps({"status": "success", "message": "AllSenses AI Guardian is LIVE!", "version": "1.0"})
    }
