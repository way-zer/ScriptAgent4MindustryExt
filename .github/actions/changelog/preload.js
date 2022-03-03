import {execSync} from "child_process";
import {dirname} from "path"

const actionDir = dirname(new URL(import.meta.url).pathname);

console.log("npm install")
execSync("npm install", {cwd: actionDir, stdio: 'inherit'})
await import("./main.js")