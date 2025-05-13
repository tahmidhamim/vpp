#!/bin/bash
for i in {1..20}
do
    curl -X POST http://localhost:8080/api/batteries -H "Content-Type: application/json" -d '[
        {"name":"Battery-'$i'-1","postcode":"6000","capacity":10000},
        {"name":"Battery-'$i'-2","postcode":"6001","capacity":20000}
    ]' &
done
wait