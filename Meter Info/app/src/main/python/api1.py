import requests
import json

def api1_lookup(meter_number):
    try:
        clean_meter = str(meter_number).strip()
        url = "http://web.bpdbprepaid.gov.bd/bn/token-check"

        headers = {
            'Accept': 'text/x-component',
            'Content-Type': 'text/plain;charset=UTF-8',
            'Next-Action': '29e85b2c55c9142822fe8da82a577612d9e58bb2',
            'Origin': 'http://web.bpdbprepaid.gov.bd',
            'Referer': 'http://web.bpdbprepaid.gov.bd/bn/token-check',
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
        }

        request_data = '[{"meterNo":"' + clean_meter + '"}]'
        response = requests.post(url, headers=headers, data=request_data, verify=False, timeout=30)

        if response.status_code == 200:
            response_text = response.text.split('\n')
            for line in response_text:
                if line.startswith('1:'):
                    api1_data = json.loads(line[2:])
                    return api1_data  # only api1_data, no consumer number
        return {}
    except Exception as e:
        return {"error": str(e)}
