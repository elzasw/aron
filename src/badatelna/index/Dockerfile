FROM blacktop/elasticsearch:7.6.2


RUN export JAVA_HOME=$(dirname "$(dirname "$(readlink -f "$(which javac || which java)")")")  \
    && /usr/share/elasticsearch/bin/elasticsearch-plugin install --batch analysis-icu