cd ..
git pull
mvn clean package -DskipTests
scp -P 8001 ./target/tool_order_search-0.0.1.jar ./deploydev/docker-compose.yaml ./deploydev/Dockerfile hoanganh.tran@103.21.149.190:./app/javaapp
ssh -p 8001 hoanganh.tran@103.21.149.190 'cd ./app/javaapp && docker-compose up -d --no-deps --build tool_order_search' 
echo "\033[1;33m.................Deployed complete ^^.................."