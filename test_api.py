import urllib.request, json

# First check what warehouses exist
r = urllib.request.urlopen("http://localhost:8080/api/warehouses")
print("warehouses: " + json.loads(r.read()).get("data",[]).__str__()[:500])

urls = [
    ("changsh overview", "http://localhost:8080/api/dashboard/overview?warehouseCode=32050005&month=2025-08"),
    ("tianjin overview", "http://localhost:8080/api/dashboard/overview?warehouseCode=12020002&month=2025-08"),
    ("changsh baseline", "http://localhost:8080/api/baseline/monthly?warehouseCode=32050005&year=2025&month=8"),
    ("changsh warehouse", "http://localhost:8080/api/baseline/warehouse/32050005?year=2025&month=8"),
]

for label, url in urls:
    try:
        r = urllib.request.urlopen(url)
        d = json.loads(r.read())
        data = d.get("data", {})
        if isinstance(data, dict):
            print(label + ": " + json.dumps(data, ensure_ascii=False)[:300])
        elif isinstance(data, list):
            for item in data:
                print(label + ": " + json.dumps(item, ensure_ascii=False)[:300])
        else:
            print(label + ": " + str(data))
    except Exception as e:
        print(label + ": ERROR " + str(e))
