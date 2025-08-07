import json
import os
import subprocess
import time
import datetime

try:
    # python3 main.py
    import git
    import gradle
except:
    # python3 pybuild/main.py
    import pybuild.git
    import pybuild.gradle

git.init_credentials("github-actions", "github-actions@github.com")
git.checkout("versioning", "orphan")
git.reset()
git.pull(branch_name="versioning")

def findPhase(ver):
    if "-a" in ver: return "alpha"
    if "-b" in ver: return "beta"
    if "-rc" in ver: return "release-candidate"
    return "release"

repoUrl = f"https://github.com/{os.getenv("GITHUB_REPO")}"

version = (os.getenv("GITHUB_REF") or "refs/tags/0.0.0-alpha").replace("refs/tags/", "")
phase = findPhase(version)

gradle.run("shadowJar")

f2 = open("gradle.properties", "w")
f2.write("""org.gradle.jvmargs=-Xmx4608M
p_version = 0.0.0-alpha""")
f2.close()

f0 = open("build/libs/mock-launcher-1.0-SNAPSHOT-all.jar", "r")
f1 = open(f"mock-launcher-{version}.jar", "x")
f1.write(f0.read())
f1.close()
f0.close()

if not os.path.exists("version.json"):
    f = open("versions.json", "x")
    f.write("""{
	"latest": {},
	"existing-phases": [],
	"versions": {}
}""")
    f.close()

f = open("versions.json", "r")
contents = f.read()
f.close()

contents = json.loads(contents)

contents["latest"]["*"] = version
contents["latest"][phase] = version

if not phase in contents["existing-phases"]:
    contents["existing-phases"].append(phase)

contents["versions"][version] = {
    "epoch": int(time.time()),
    "date": datetime.datetime.now().astimezone(datetime.timezone.utc).strftime("%Y/%m/%dT%H:%M:%S"),
    "id": version,
    "phase": phase
}

contents["versions"][version]["download"] = f"{repoUrl}/releases/download/{version}/mock-launcher-{version}.jar"
subprocess.call(args=["gh", "release", "upload", version, f"./mock-launcher-{version}.jar;"])

f = open("versions.json", "w")
f.write(json.dumps(contents, indent="\t"))
f.close()

git.commit(f"add {version} to version manifest", "versions.json")
git.push(branch_name="versioning")