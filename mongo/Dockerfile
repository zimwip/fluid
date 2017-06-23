FROM mongo:latest

ENV CONTAINERPILOT_VERSION 2.4.3
RUN apt-get update && apt-get install -y  --no-install-recommends  \
		make \
		openssl \
		unzip \
		curl \
    && rm -rf /var/lib/apt/lists/* \
	&& curl -k -L -o containerpilot.tar.gz "https://github.com/joyent/containerpilot/releases/download/${CONTAINERPILOT_VERSION}/containerpilot-${CONTAINERPILOT_VERSION}.tar.gz" \
	&& tar -xzf containerpilot.tar.gz -C /usr/local/bin \
	&& rm -r containerpilot.tar.gz \
	&& curl -k -L -o /tmp/consul.zip https://releases.hashicorp.com/consul/0.7.0/consul_0.7.0_linux_amd64.zip \
	&& unzip /tmp/consul -d /usr/local/bin \
	&& mkdir /config \
	&& mkdir /consul-data 

COPY mongo-consul.json /etc/mongo-consul.json
ENV CONTAINERPILOT=file:///etc/mongo-consul.json

EXPOSE 27017
ENTRYPOINT [ "/usr/local/bin/containerpilot", "mongod"]
