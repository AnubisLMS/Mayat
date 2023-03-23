import sys
import json
import base64
from flask import Flask, render_template

app = Flask(__name__)
app.jinja_env.globals.update(b64=base64.urlsafe_b64encode)
data = None

@app.route("/")
def main():
    return render_template("main.html", data=data)

@app.route("/<int:index>")
def result(index):
    entry = data["result"][index - 1]

    with open(entry["submission_A"], 'r') as f:
        submission_A_code = f.read()
    with open(entry["submission_B"], 'r') as f:
        submission_B_code = f.read()

    return render_template(
        "result.html",
        entry=entry,
        submission_A_code=submission_A_code,
        submission_B_code=submission_B_code,
        prev=f"/{index - 1 if index > 1 else index}",
        next=f"/{index + 1 if index < len(data['result']) else index}"
    )

if __name__ == "__main__":
    data = json.loads(open(sys.argv[1]).read())
    data["result"].sort(key=lambda x: -x["similarity"])
    app.run(debug=True)