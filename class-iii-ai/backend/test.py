import requests
import os

def test_analyze():
    # Create a dummy image file
    with open("dummy.jpg", "wb") as f:
        f.write(b"fake image content")
        
    url = "http://127.0.0.1:8000/analyze"
    print(f"Testing POST {url} ...")
    
    try:
        with open("dummy.jpg", "rb") as f:
             response = requests.post(url, files={"file": f})
        
        print("Status:", response.status_code)
        print("Response:", response.json())
    except Exception as e:
        print("Failed to connect or test:", e)
    finally:
        os.remove("dummy.jpg")

if __name__ == "__main__":
    test_analyze()
