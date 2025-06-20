from confluent_kafka import Consumer, KafkaError
import json
import threading
from typing import Callable, Dict


class KafkaMovieEventConsumer:
    def __init__(self, bootstrap_servers: str, group_id: str, topic: str, on_event: Callable):
        self.conf = {
            'bootstrap.servers': bootstrap_servers,
            'group.id': group_id,
            'auto.offset.reset': 'earliest'
        }
        self.topic = topic
        self.on_event = on_event
        self.consumer = Consumer(self.conf)
        self.running = False
        self.thread = None

    def start(self):
        """Start the Kafka consumer in a separate thread"""
        if self.running:
            return

        self.running = True
        self.consumer.subscribe([self.topic])
        self.thread = threading.Thread(target=self._consume_loop)
        self.thread.daemon = True
        self.thread.start()

    def _consume_loop(self):
        """Main consumption loop"""
        try:
            while self.running:
                msg = self.consumer.poll(1.0)
                if msg is None:
                    continue
                if msg.error():
                    if msg.error().code() == KafkaError._PARTITION_EOF:
                        continue
                    else:
                        print(f"Kafka error: {msg.error()}")
                        break

                try:
                    # Process message
                    value = msg.value().decode('utf-8')
                    event = json.loads(value)
                    self.on_event(event)
                except Exception as e:
                    print(f"Error processing message: {e}")
        finally:
            self.consumer.close()

    def stop(self):
        """Stop the Kafka consumer"""
        self.running = False
        if self.thread:
            self.thread.join(timeout=5.0)
            self.thread = None
