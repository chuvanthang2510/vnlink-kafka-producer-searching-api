FROM docker.elastic.co/logstash/logstash:7.17.0

# Copy pipeline config
COPY pipeline/logstash.conf /usr/share/logstash/pipeline/logstash.conf

# Copy input data file
COPY data/data.json /usr/share/logstash/data/data.json
