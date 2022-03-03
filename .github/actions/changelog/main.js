import * as core from "@actions/core";
import {context, getOctokit} from "@actions/github";

let token = process.env.GITHUB_TOKEN || core.getInput("token")
let octokit = getOctokit(token)

let lastRelease = (await octokit.rest.repos.getLatestRelease(context.repo)).data.tag_name
core.info("Find last release: " + lastRelease)

let commits = (await octokit.rest.repos.listCommits({
    ...context.repo, since: lastRelease
})).data
let changes = commits.map(commit => {
    let {sha, author, commit: {message}} = commit
    let [title, ...body] = message.split("\n")

    let out = `* ${title} @${author.login} (${sha.substring(0, 8)})`
    if (body.length)
        out += body.map(it => `\n  > ${it}`).join("")
    return out
}).join("\n")
core.setOutput("changes", changes)

core.setOutput("releaseBody", `
# 预发布版本，仅供测试使用
---
## 更新日记
${changes}
`)