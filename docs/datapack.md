# BingoCook 数据包自定义内容

为 BingoCook 添加自定义**元素**/**物品分配元素值**/**烹饪配方**/**调味品修正**/**热源方块**。

环境要求：安装了 BingoCook 模组的最新版本

---

## 1. 机制

烹饪锅以 3x3（9 格）输入 + 1 格输出进行烹饪：

- 每件食材带有**元素值**（如 `bingocook:fruit: 2`）
- **配方**规定元素总量要求（min/max、allowed 白名单），9 格全满且满足要求时开始烹饪
- 配方可定义**调味品**（seasonings）：输入中的调味品会修正产出菜肴的属性
- 烹饪锅正下方需为配置的**有效热源**（如点燃的营火）

---

## 2. 数据包结构

### 2.1 在 `saves/<世界>/datapacks/` 下建立数据包

```
mycook/
└── pack.mcmeta          # pack_format: 107（26.2）
└── data/
    ├── mymod/           # 你的命名空间
    │   └── bingocook/
    │       ├── element_types.json      # 元素类型
    │       └── heat_sources.json       # 烹饪热源
    └── bingocook/       # 覆盖模组自带数据时使用（见 §6）
        ├── data_maps/item/item_elements.json
        └── recipe/xxx.json
```

### 2.2 `pack.mcmeta`格式

```json
{
  "pack": {
    "description": "My BingoCook pack",
    "pack_format": 107
  }
}
```

### 使用KubeJS建立数据包

如果KubeJS模组已经移植到了本模组支持的Minecraft版本，用它加载数据包会更好。

```
kubejs/
└── startup_scripts
└── server_scripts
└── client_scripts
└── data/
    ├── mymod/           # 你的命名空间
    │   └── bingocook/
    │       ├── element_types.json      # 元素类型
    │       └── heat_sources.json       # 烹饪热源
    └── bingocook/       # 覆盖模组自带数据时使用（见 §6）
        ├── data_maps/item/item_elements.json
        └── recipe/xxx.json
```

---

## 3. 元素类型

### 3.1 添加新元素

`data/<你的命名空间>/bingocook/element_types.json`：

```json
{
  "replace": false,
  "values": ["mymod:spicy"]
}
```

- `replace: false`（默认）：向现有集合**追加**新元素，而不是删除模组原有的元素后再新建列表中的元素。

### 3.2 清空并重建元素集合

```json
{
  "replace": true,
  "values": ["mymod:spicy", "mymod:sweet"]
}
```

删除模组原有的元素后再新建列表中的元素。

**注意**：元素集合与物品元素值相互独立——清空元素不会删除物品上的元素值数据，只是配方无法再引用被移除的元素。

### 3.3 删除元素

把模组自带文件以**同路径**放进数据包并 `replace: true` 重建（无法单独删除一个元素，只能重建集合）。
删除某元素后，引用它的物品元素值成为"未知元素"。
删除元素后，残留其值的物品将无法参与任何带白名单的配方，直到你清理对应物品的元素值。

---

## 4. 热源配置

文件：`data/<命名空间>/bingocook/heat_sources.json`

烹饪锅正下方一格必须为**有效热源**才会推进烹饪进度；热源丢失（被破坏或熄灭）时进度立即重置。

### 4.1 添加热源

```json
{
  "replace": false,
  "values": ["minecraft:lava", "minecraft:magma_block"]
}
```

- 条目为**方块 ID**（`minecraft:lava`）或**方块标签**（`#minecraft:campfires`）
- 若方块状态含 `lit` 属性（篝火、熔炉等），则要求 `lit=true`；无 `lit` 属性的方块（岩浆等）放置即有效
- 合并语义与 §3 元素类型相同

模组默认热源：`minecraft:campfire`、`minecraft:soul_campfire`（需点燃）。

### 4.2 删除/重建热源

与 §3 相同

### 4.3 运行时命令（不写入数据包）

管理员可用命令临时增删热源，**`/reload` 后恢复为数据包默认值**：

```
/bingocook heat_sources list
/bingocook heat_sources add minecraft:lava
/bingocook heat_sources remove minecraft:campfire
```

---

## 5. 物品元素值（Data Map）

文件：`data/<命名空间>/data_maps/item/item_elements.json`（Data Map ID：`bingocook:item_elements`）。

**注意目录是 `data_maps` 而非 `data_map`。**

### 5.1 为物品添加/修改元素值

```json
{
  "values": {
    "minecraft:apple": { "elements": { "bingocook:fruit": 2 } },
    "minecraft:beef": { "elements": { "bingocook:meat": 3, "bingocook:seasoning": 1 } },
    "mymod:chili": { "elements": { "mymod:spicy": 3 } }
  }
}
```

- 值结构固定为 `{"elements": { <元素id>: <整数> }}`
- 元素值为 0 等价于缺失
- 同一物品 id 出现在多个文件中时，后加载者整条覆盖

### 5.2 为整组物品赋值（tag）

```json
{
  "values": {
    "#minecraft:logs": { "elements": { "mymod:wood": 1 } }
  }
}
```

### 5.3 删除物品的元素值

```json
{
  "values": {},
  "remove": ["minecraft:potato"]
}
```

---

## 6. 自定义配方

文件：`data/<命名空间>/recipe/<配方名>.json`。

**目录是单数 `recipe/`**——写成复数 `recipes/` 会被忽略且不报错提示。

### 6.1 示例

```json
{
  "type": "bingocook:cooking",
  "allowed": ["bingocook:vegetable", "bingocook:fruit", "bingocook:seasoning"],
  "requirements": {
    "bingocook:vegetable": { "min": 1 },
    "bingocook:fruit": { "min": 1 }
  },
  "cookingTime": 600,
  "result": { "id": "minecraft:bread", "count": 1 }
}
```

### 6.2 字段说明

| 字段 | 类型 | 说明 |
|---|---|---|
| `type` | string | `"bingocook:cooking"` |
| `allowed` | string[] | **白名单**：允许出现的元素 id；省略 = 全部允许；不在白名单中的元素总量必须为 0。|
| `requirements` | object | 各元素总量要求：`{ "min": n, "max": n }`，省略的项不限制（min 默认 0，max 默认无穷）。9 格元素值**求和**后判定 |
| `cookingTime` | int | 烹饪所需 tick（20 tick = 1 秒）|
| `result` | object | 产出物品：`{ "id": "...", "count": n }`（count 默认 1） |
| `seasonings` | object | （可选）调味品修正 |
| `enabled` | bool | 默认 true；false 时禁用该配方 |

### 6.3 调味品修正（seasonings）

```json
{
  "type": "bingocook:cooking",
  "allowed": ["bingocook:vegetable", "bingocook:fruit", "bingocook:seasoning"],
  "requirements": { "bingocook:vegetable": { "min": 1 }, "bingocook:fruit": { "min": 1 } },
  "cookingTime": 600,
  "result": { "id": "mymod:veggie_stew", "count": 1 },
  "seasonings": {
    "minecraft:sugar": {
      "nutrition": 1,
      "saturation": 1,
      "effects": [
        { "effect": "minecraft:instant_health", "duration": 1, "amplifier": 0, "probability": 1.0 }
      ],
      "permanentAttributes": [
        { "attribute": "minecraft:max_health", "amount": 1, "operation": "add_value" }
      ]
    }
  }
}
```

规则：

- 输入 9 格中**命中 seasonings 的物品按种类去重**，每种只生效一次（3 个糖只算 1 个糖的修正）
- `nutrition`/`saturation`：对基础值做**原始加法**（可为负数，会直接加减营养/饱食度）
- `effects`：附加食用效果（`effect` + `duration`（tick）+ `amplifier` + `probability`）。回血用 `minecraft:instant_health`（粒度为 4 点/级：amplifier 0 = 4 点，1 = 8 点）
- `permanentAttributes`：**永久属性**——食用后经 `AttributeMap.addPermanentModifier` 永久生效，随玩家 NBT 持久化、重生保留
  - `operation`：`add_value`（直接加数值）/ `add_multiplied_base`（按倍率线性加数值）/ `add_multiplied_total`（按倍率复利加数值）
  - 可参考§9.1的例子进行理解
- 调味品物品本身需要有 `seasoning` 元素值（或能通过配方 allowed）才能放入锅；**有元素值但未在配方 seasonings 中定义的物品只作占位，无任何修正**

### 6.4 模组自带配方

模组自带配方位于 `data/bingocook/recipe/`：

- `vegetable_fruit_stew.json`：蔬果乱炖（含糖调味品示范，输出 `bingocook:vegetable_fruit_stew`）
- `meat_vegetable_stew.json`：肉菜炖（无调味品最简示范，输出 `bingocook:meat_vegetable_stew`）

---

## 7. 覆盖模组自带数据

数据包优先级：后加载的包覆盖先加载的包（同一数据包内后写的文件覆盖先写的）。覆盖模组内容时，把文件放到**与模组相同的路径**：

| 要覆盖的内容 | 模组文件路径 | 你的数据包路径 |
|---|---|---|
| 元素集合 | `data/bingocook/bingocook/element_types.json` | 同路径 |
| 热源 | `data/bingocook/bingocook/heat_sources.json` | 同路径 |
| 物品元素值 | `data/bingocook/data_maps/item/item_elements.json` | 同路径 |
| 配方 | `data/bingocook/recipe/<配方名>.json` | 同路径 |

### 7.1 修改配方

同路径覆盖整个文件（`allowed`/`requirements`/`cookingTime`/`result` 等全部重写）：

```json
{
  "type": "bingocook:cooking",
  "allowed": ["bingocook:vegetable", "bingocook:fruit", "bingocook:seasoning"],
  "requirements": { "bingocook:vegetable": { "min": 3 }, "bingocook:fruit": { "min": 2 } },
  "cookingTime": 1200,
  "result": { "id": "bingocook:vegetable_fruit_stew", "count": 1 }
}
```

### 7.2 禁用配方

同路径覆盖为仅含 `enabled: false`（其余字段可保留原值）：配方加载后禁用，`/bingocook elements recipe` 会显示 `(disabled)`：

```json
{
  "type": "bingocook:cooking",
  "enabled": false
}
```

---

## 8. 查询与调试

权限要求：`LEVEL_GAMEMASTERS`（管理员）。

```
# 列出全部元素
/bingocook elements list

# 查询物品元素值（无元素时输出 No elements）
/bingocook elements item minecraft:apple

# 查询配方要求（allowed / 各元素 min-max / 时长 / 调味品 / disabled 状态）
/bingocook elements recipe bingocook:vegetable_fruit_stew

# 列出当前有效热源（标注 datapack / runtime 来源）
/bingocook heat_sources list
```

修改数据后执行 `/reload`（对元素类型、热源、Data Map、配方四类数据全部生效；命令临时增删的热源也会在此刻恢复为数据包默认值）。

---

## 9. 补充

### 9.1 operation 例子

- 假设某料理可以使max_health增加，且玩家的基础max_health为20

- operation为add_value，amount为2，食用三次该料理后玩家的max_health为26（20 + 2 x 3）
- operation为add_multiplied_base，amount为2，食用三次该料理后玩家的max_health为140（20 x（1 + 2 x 3））
- operation为add_multiplied_total，amount为0.1，食用三次该料理后玩家的max_health为540（20 x (（1 + 2）^3 )）

### 9.2 陷阱
1. **目录单数**：配方必须用 `recipe/`，战利品表用 `loot_table/`。复数目录被**静默忽略、无任何报错**——写完配方后先 `elements recipe` 确认加载，或看启动日志的配方总数
2. **`elements` 包装键**：Data Map 值必须是 `{"elements": {...}}`，不是直接的元素对象
3. **allowed 白名单**：不写 `allowed` 意味着全部元素允许。想要"无其它元素"（如纯素炖），必须显式列出允许的元素
4. **元素值求和**：配方要求针对 9 格**总量**，不是单格。一个格子放 2 个苹果（fruit:2 各计 1 个物品）——求和按每格物品的元素值相加
5. **调味品去重**：同种调味品只生效一次，堆数量不会叠加修正
6. **永久属性可重复获取**：每份带永久属性修正的菜肴食用都会生效，这是设计特性（可无限刷属性）
7. **跨调味批次不堆叠**：输出槽存有"含糖调味"菜肴时，同一配方"无调味"批次的产出（组件不同）不会与其堆叠，烹饪会暂停——取走输出即恢复
8. **物品 id 合法性**：`values` 与 `result` 引用的物品必须是已注册物品，否则配方/数据映射加载报错
