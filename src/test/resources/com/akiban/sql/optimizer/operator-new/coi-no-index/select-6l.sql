SELECT customers.name, order_date FROM customers LEFT OUTER JOIN (orders INNER JOIN items ON orders.oid = items.oid) ON customers.cid = orders.cid WHERE quan > 100 ORDER BY order_date DESC LIMIT 10