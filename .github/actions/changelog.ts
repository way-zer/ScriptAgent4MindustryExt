import * as core from "npm:@actions/core"
import {context, getOctokit} from "npm:@actions/github";


const token = Deno.env.get("GITHUB_TOKEN") || core.getInput("token")
const octokit = getOctokit(token)

const lastRelease = (await octokit.rest.repos.getLatestRelease(context.repo)).data.tag_name
core.info("Find last release: " + lastRelease)

const compare = (await octokit.rest.repos.compareCommits({
    ...context.repo, base: lastRelease, head: context.sha
})).data

const changes = compare.commits.map(({sha, author, commit: {message}}) => {
    const [title, ...body] = message.split("\n")

    let out = `* ${title} @${author!.login} (${sha.substring(0, 8)})`
    if (body.length)
        out += body.map(it => `\n  > ${it}`).join("")
    return out
}).join("\n")
core.setOutput("changes", changes)

const changeFiles = (compare.files || []).map(file => {
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
}).join("\n")

core.setOutput("releaseBody", `
    ## 预发布版本，仅供测试使用

    正式发布前，可能会多次更新(以标题build号为准)

    ---

    ## 更新日记

    ${changes}

    ## 文件变更

    <details>
    <summary>${compare.files?.length || 0} 文件</summary>

    ${changeFiles}

    </details>

    [完整对比](${compare.html_url}) [获取patch](${compare.patch_url})
`)