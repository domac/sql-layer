SELECT this_.id AS id1_1_1_,
this_.description AS descript2_1_1_,
this_.body_weight AS body_wei3_1_1_,
this_.mother_id AS mother_i4_1_1_,
this_.father_id AS father_i5_1_1_,
this_.zoo_id AS zoo_id6_1_1_,
this_.serial_number AS serialNu7_1_1_,
this_1_.bodytemperature AS bodyTemp2_20_1_,
this_3_.pregnant AS pregnant2_17_1_,
this_3_.birthdate AS birthdat3_17_1_,
this_4_.owner AS owner2_9_1_,
this_7_.name_first AS name_fir2_11_1_,
this_7_.name_initial AS name_ini3_11_1_,
this_7_.name_last AS name_las4_11_1_,
this_7_.nickname AS nickName5_11_1_,
this_7_.height_centimeters / 2.54E0 AS height_c6_11_1_,
this_7_.intvalue AS intValue7_11_1_,
this_7_.floatvalue AS floatVal8_11_1_,
this_7_.bigdecimalvalue AS bigDecim9_11_1_,
this_7_.bigintegervalue AS bigInte10_11_1_,
CASE
WHEN this_2_.reptile IS NOT NULL THEN 2
WHEN this_5_.mammal IS NOT NULL THEN 5
WHEN this_6_.mammal IS NOT NULL THEN 6
WHEN this_4_.mammal IS NOT NULL THEN 4
WHEN this_7_.mammal IS NOT NULL THEN 7
WHEN this_1_.animal IS NOT NULL THEN 1
WHEN this_3_.animal IS NOT NULL THEN 3
WHEN this_.id IS NOT NULL THEN 0
END AS clazz_1_,
m1_.id AS id1_1_0_,
m1_.description AS descript2_1_0_,
m1_.body_weight AS body_wei3_1_0_,
m1_.mother_id AS mother_i4_1_0_,
m1_.father_id AS father_i5_1_0_,
m1_.zoo_id AS zoo_id6_1_0_,
m1_.serial_number AS serialNu7_1_0_,
m1_1_.bodytemperature AS bodyTemp2_20_0_,
m1_3_.pregnant AS pregnant2_17_0_,
m1_3_.birthdate AS birthdat3_17_0_,
m1_4_.owner AS owner2_9_0_,
m1_7_.name_first AS name_fir2_11_0_,
m1_7_.name_initial AS name_ini3_11_0_,
m1_7_.name_last AS name_las4_11_0_,
m1_7_.nickname AS nickName5_11_0_,
m1_7_.height_centimeters / 2.54E0 AS height_c6_11_0_,
m1_7_.intvalue AS intValue7_11_0_,
m1_7_.floatvalue AS floatVal8_11_0_,
m1_7_.bigdecimalvalue AS bigDecim9_11_0_,
m1_7_.bigintegervalue AS bigInte10_11_0_,
CASE
WHEN m1_2_.reptile IS NOT NULL THEN 2
WHEN m1_5_.mammal IS NOT NULL THEN 5
WHEN m1_6_.mammal IS NOT NULL THEN 6
WHEN m1_4_.mammal IS NOT NULL THEN 4
WHEN m1_7_.mammal IS NOT NULL THEN 7
WHEN m1_1_.animal IS NOT NULL THEN 1
WHEN m1_3_.animal IS NOT NULL THEN 3
WHEN m1_.id IS NOT NULL THEN 0
END AS clazz_0_
FROM animal this_
LEFT OUTER JOIN reptile this_1_
ON this_.id = this_1_.animal
LEFT OUTER JOIN lizard this_2_
ON this_.id = this_2_.reptile
LEFT OUTER JOIN mammal this_3_
ON this_.id = this_3_.animal
LEFT OUTER JOIN domesticanimal this_4_
ON this_.id = this_4_.mammal
LEFT OUTER JOIN cat this_5_
ON this_.id = this_5_.mammal
LEFT OUTER JOIN dog this_6_
ON this_.id = this_6_.mammal
LEFT OUTER JOIN human this_7_
ON this_.id = this_7_.mammal
INNER JOIN animal m1_
ON this_.mother_id = m1_.id
LEFT OUTER JOIN reptile m1_1_
ON m1_.id = m1_1_.animal
LEFT OUTER JOIN lizard m1_2_
ON m1_.id = m1_2_.reptile
LEFT OUTER JOIN mammal m1_3_
ON m1_.id = m1_3_.animal
LEFT OUTER JOIN domesticanimal m1_4_
ON m1_.id = m1_4_.mammal
LEFT OUTER JOIN cat m1_5_
ON m1_.id = m1_5_.mammal
LEFT OUTER JOIN dog m1_6_
ON m1_.id = m1_6_.mammal
LEFT OUTER JOIN human m1_7_
ON m1_.id = m1_7_.mammal
WHERE CASE
WHEN m1_2_.reptile IS NOT NULL THEN 2
WHEN m1_5_.mammal IS NOT NULL THEN 5
WHEN m1_6_.mammal IS NOT NULL THEN 6
WHEN m1_4_.mammal IS NOT NULL THEN 4
WHEN m1_7_.mammal IS NOT NULL THEN 7
WHEN m1_1_.animal IS NOT NULL THEN 1
WHEN m1_3_.animal IS NOT NULL THEN 3
WHEN m1_.id IS NOT NULL THEN 0
END = 1
