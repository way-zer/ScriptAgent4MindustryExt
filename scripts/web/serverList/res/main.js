const table = layui.table;

function escapeHtml(unsafe) {
    return unsafe.toString()
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}

function green(text) {
    return `<a style="color: green">${escapeHtml(text)}</a>`
}

function warn(text) {
    return `<a style="color: darkred">${escapeHtml(text)}</a>`
}

function colorize(text) {
    function colors(str) {
        if (str.match(/#[0-9a-f]{3,8}/i)) return str;
        else {
            const colors = {
                '': 'black',

                white: 'black',
                lightgray: '#bfbfbfff',
                gray: '#7f7f7fff',
                darkgray: '#3f3f3fff',
                black: 'black',
                clear: 'black',

                blue: '#0000ffff',
                navy: '#00007fff',
                royal: '#4169e1ff',
                slate: '#708090ff',
                sky: '#87ceebff',
                cyan: '#00ffffff',
                teal: '#007f7fff',

                green: '#00ff00ff',
                acid: '#7fff00ff',
                lime: '#32cd32ff',
                forest: '#228b22ff',
                olive: '#6b8e23ff',

                yellow: '#ffff00ff',
                gold: '#ffd700ff',
                goldenrod: '#daa520ff',
                orange: '#ffa500ff',

                brown: '#8b4513ff',
                tan: '#d2b48cff',
                brick: '#b22222ff',

                red: '#ff0000ff',
                scarlet: '#ff341cff',
                coral: '#ff7f50ff',
                salmon: '#fa8072ff',
                pink: '#ff69b4ff',
                magenta: '#7f007fff',

                purple: '#a020f0ff',
                violet: '#ee82eeff',
                maroon: '#b03060ff',

                // alias?
                crimson: '#ff341cff', // scarlet

                // special
                accent: '#ffcb39ff'
            };
            return colors[str.toLowerCase()]
        }
    }

    let lastColor = "black";
    return text.replace(/\[([#0-9a-zA-Z]*)]([^\[]*)/g, (_match, color, text) => {
        let resolved_color = colors(color);
        if (resolved_color) {
            lastColor = resolved_color;
            return `<span style="color: ${lastColor}">${escapeHtml(text)}</span>`;
        } else {
            return `<span style="color: ${lastColor}">${escapeHtml(_match)}</span>`;
        }
    })
}

let maxVersion = 0;
table.render({
    elem: "#main",
    url: "/servers/list",
    initSort: {
        field: 'players', type: 'desc'
    },
    cols: [[
        {field: 'address', title: '地址', minWidth: 140, sort: true, fixed: 'left'},
        {
            field: 'name', title: '名字', minWidth: 260, templet(d) {
                return colorize(d.name);
            }
        },
        {
            field: 'description', title: '介绍', minWidth: 260, templet(d) {
                return colorize(d.description);
            }
        },
        {
            field: 'mapName', title: '地图名', minWidth: 160, templet(d) {
                return colorize(d.mapName);
            }
        },
        {field: 'wave', title: '波数'},
        {
            field: 'mode', title: '模式', templet(d) {
                switch (d.mode) {
                    case "survival":
                        return "生存";
                    case "sandbox":
                        return "沙盒";
                    case "attack":
                        return "攻城";
                    case "pvp":
                        return "PVP";
                    case "editor":
                        return "编辑器";
                }
            }
        },
        {field: 'players', title: '玩家数', minWidth: 90, sort: true},
        {
            field: 'limit', title: '玩家上限', minWidth: 86, templet(d) {
                if (d.limit === 0) return green("无限制");
                else return warn(d.limit);
            }
        },
        {
            field: 'version', title: '版本', templet(d) {
                if (d.version >= maxVersion) return green(d.version);
                else return warn(d.version);
            }
        },
        {
            field: 'type', title: '类型', templet(d) {
                if (d.type === "official") return green("原版");
                else return warn(d.type);
            }
        },
        {field: 'timeMs', title: '延迟(ms)', minWidth: 110, sort: true},
        {
            field: 'online', title: '状态', templet(d) {
                if (d.online === true) return green("在线");
                else return warn("离线");
            }
        },
        {
            field: 'lastUpdate', title: '最后更新时间', minWidth: 130, templet(d) {
                return ((Date.now() - d.lastUpdate) / 1000).toFixed(1) + " 秒前"
            }
        },
    ]],
    parseData(res) {
        res.forEach(e => {
            maxVersion = Math.max(maxVersion, e.version)
        });
        return {
            code: 0,
            msg: "",
            count: res.length,
            data: res,
        }
    }
});

const layer = layui.layer;
const $ = layui.$;
$("#openAdd").click(function () {
    layer.prompt({
        title: "请输入服务器地址",
    }, (v, prompt) => {
        const loading = layer.load();
        $.get("/servers/add?address=" + v, (data, status) => {
            layer.alert("添加成功", {time: 3});
            table.reload("main");
            layer.close(prompt);
            layer.close(loading);
        }).error((jqXHR, textStatus, exception) => {
            if (jqXHR.status === 406) {
                layer.alert("连接失败,请检查地址是否正确")
            } else {
                layer.alert("添加失败: " + textStatus);
                console.error(exception)
            }
            layer.close(loading);
        });
    });
});
setInterval(() => {
    table.reload("main")
}, 60 * 1000);