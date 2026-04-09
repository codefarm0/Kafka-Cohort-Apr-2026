## API curl request

``
curl --location 'http://localhost:8080/api/orders' \
--header 'Content-Type: application/json' \
--data '{
    "customerId" :"111",
    "productId":"321",
    "quantity":3,
    "totalAmount" : 200.5
}'
``