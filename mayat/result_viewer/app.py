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

@app.route("/<submission_A>/<submission_B>")
def result(submission_A, submission_B):
    submission_A = base64.urlsafe_b64decode(submission_A).decode()
    submission_B = base64.urlsafe_b64decode(submission_B).decode()
    return render_template("result.html", submission_A=submission_A, submission_B=submission_B)

if __name__ == "__main__":
    data = json.loads(open(sys.argv[1]).read())
    data["result"].sort(key=lambda x: -x["similarity"])
    app.run(debug=True)