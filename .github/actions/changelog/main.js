import * as core from "@actions/core";
import {context, getOctokit} from "@actions/github";

let token = process.env.GITHUB_TOKEN || core.getInput("token")
let octokit = getOctokit(token)

let lastRelease = (await octokit.rest.repos.getLatestRelease(context.repo)).data.tag_name
core.info("Find last release: " + lastRelease)

let compare = (await octokit.rest.repos.compareCommits({
    ...context.repo, base: lastRelease, head: context.sha
})).data

let changes = compare.commits.map(commit => {
    let {sha, author, commit: {message}} = commit
    let [title, ...body] = message.split("\n")

    let out = `* ${title} @${author.login} (${sha.substring(0, 8)})`
    if (body.length)
        out += body.map(it => `\n  > ${it}`).join("")
    return out
}).join("\n")
core.setOutput("changes", changes)

let changeFiles = compare.files.map(file => {
    switch (file.status) {
        case 'modified':
            return `* :memo: ${file.filename} +${file.additions} -${file.deletions}`
        case 'added':
            return `* :heavy_plus_sign: ${file.filename}`
        case "removed":
            return `* :fire: ${file.filename}`
        case "renamed":
            return `* :truck: ${file.filename} <= ${file.previous_filename}`
        default:
            return `* ${file.status} ${file.filename}`
    }
})

core.setOutput("releaseBody", `
# 预发布版本，仅供测试使用;正式发布前，可能会多次更新(以build号为准)
---
## 更新日记
${changes}
## 文件变更
${changeFiles}
[完整对比](${compare.html_url}) [完整patch](${compare.patch_url})
`)