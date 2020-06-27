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
                lightgray: '#898989',
                gray: '#7f7f7fff',
                darkgray: '#3f3f3fff',
                black: 'black',
                clear: 'black',

                blue: '#0000ffff',
                navy: '#00007fff',
                royal: '#4169e1ff',
                slate: '#708090ff',
                sky: '#6eafc4',
                cyan: '#00b3b3',
                teal: '#007f7fff',

                green: '#009900',
                acid: '#59b300',
                lime: '#259925',
                forest: '#228b22ff',
                olive: '#6b8e23ff',

                yellow: '#bfbf00',
                gold: '#bf9f00',
                goldenrod: '#daa520ff',
                orange: '#cc8500',

                brown: '#8b4513ff',
                tan: '#d2b48c',
                brick: '#b22222ff',

                red: '#bf0000',
                scarlet: '#ff341cff',
                coral: '#cc653f',
                salmon: '#cc695e',
                pink: '#cc5490',
                magenta: '#7f007fff',

                purple: '#a020f0',
                violet: '#cc70cc',
                maroon: '#b03060ff',

                // alias?
                crimson: '#bf2615', // scarlet

                // special
                accent: '#bf972a'
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
    // initSort: {
    //     field: 'players', type: 'desc'
    // },
    cols: [[
        {
            field: 'address', title: '地址', minWidth: 170, sort: true, fixed: 'left', templet(d) {
                return `<i class="layui-icon layui-icon-circle-dot" style="padding-right: 0.5rem;color: ${d.online ? "green" : "red"}"></i>${d.address}`;
            }
        },
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
            field: 'lastOnline', title: '最后在线时间', minWidth: 130, templet(d) {
                return ((Date.now() - d.lastOnline) / 1000).toFixed(1) + " 秒前"
            }
        },
    ]],
    parseData(res) {
        res.forEach(e => {
            if (e.version < 1000)
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
            } else if (jqXHR.status === 403) {
                layer.alert("错误: " + jqXHR.responseText);
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