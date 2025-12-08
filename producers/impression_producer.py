#!/usr/bin/env python3
import json
import time
import random
from datetime import datetime
from kafka import KafkaProducer

class ImpressionProducer:
    def __init__(self, bootstrap_servers=None):
        if bootstrap_servers is None:
            bootstrap_servers = ['localhost:19092', 'localhost:19093', 'localhost:19094']

        self.producer = KafkaProducer(
            bootstrap_servers=bootstrap_servers,
            value_serializer=lambda v: json.dumps(v).encode('utf-8'),
            key_serializer=lambda k: k.encode('utf-8'),
            acks='all',
            retries=3,
            retry_backoff_ms=100,
            # Performance Tuning for High Throughput
            linger_ms=10,        # Wait up to 10ms to batch messages
            batch_size=16384 * 4  # 64KB batch size
        )

        self.users = ['user1', 'user2', 'user3', 'user4', 'user5']
        self.products = ['product1', 'product2', 'product3', 'product4', 'product5']
        self.session_counter = 1000
    
    def generate_impression_event(self):
        current_time = int(datetime.now().timestamp() * 1000)
        user_id = random.choice(self.users)
        product_id = random.choice(self.products)
        
        event = {
            "user_id": user_id,
            "product_id": product_id,
            "timestamp": current_time,
            "event_type": "impression",
            "session_id": f"session_{self.session_counter + random.randint(1, 100)}"
        }
        return event
    
    def run(self):
        print("Starting impression producer (High Throughput Mode)...")
        
        try:
            count = 0
            start_time = time.time()
            
            while True:
                event = self.generate_impression_event()
                
                # Send to Kafka (async)
                self.producer.send(
                    'impressions',
                    key=event['product_id'],
                    value=event
                )
                
                count += 1
                
                # Log stats every 5000 records to avoid I/O bottleneck
                if count % 5000 == 0:
                    elapsed = time.time() - start_time
                    rate = count / elapsed
                    print(f"[{datetime.now().strftime('%H:%M:%S')}] Sent {count} impressions. Rate: {rate:.2f} msg/sec")
                
                # No sleep for max throughput
                
        except KeyboardInterrupt:
            print("\nStopping impression producer...")
        finally:
            self.producer.close()

if __name__ == "__main__":
    import os
    bootstrap_servers_env = os.environ.get('BOOTSTRAP_SERVERS')
    if bootstrap_servers_env:
        producer = ImpressionProducer(bootstrap_servers=bootstrap_servers_env.split(','))
    else:
        producer = ImpressionProducer()
    producer.run()