#!/bin/bash
BLOCK=$1
QUEUE="https://sqs.us-east-2.amazonaws.com/984148963792/blocks.fifo"
DID=$(head /dev/urandom | tr -dc 'a-zA-Z0-9' | fold -w 32 | head -n 1)
aws sqs send-message --message-body $BLOCK --queue-url $QUEUE --message-group-id none --message-deduplication-id $DID
