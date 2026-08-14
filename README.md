# Refine Tool (Fabric, Minecraft 1.21.11)

Bloco de refino que conserta ferramentas, armas e elytras gastando **bem
pouco material** (bem mais barato que uma bigorna), com o item sendo
exibido sobre a bancada enquanto e consertado. Modelo convertido do seu
`refine_tool.geo.json` / `refine_tool.png`.

## ⚠️ Leia isto antes de compilar

A 1.21.11 é uma versão **extremamente recente** (lançada bem depois do meu
conhecimento confiável de treinamento) e foi uma atualização gigante: a
Mojang renomeou `ResourceLocation` para `Identifier`, reescreveu boa parte
da pipeline de renderização, e mudou a API de NBT (`CompoundTag`) para
retornar `Optional` em vez dos valores diretos. Eu pesquisei e corrigi tudo
isso que consegui verificar, mas há uma área que **não consigo garantir
100%** sem compilar de verdade:

**`RefineBlockEntityRenderer.java`** — o sistema de renderização de block
entities mudou bastante entre a 1.21.1 e a 1.21.9/1.21.11 (um novo padrão
`BlockEntityRenderer<T, RenderState>` com `SubmitNodeCollector` e
`ItemStackRenderState` substituindo partes do sistema antigo). Escrevi esse
arquivo usando a API "classica" (`BlockEntityRenderer<T>` com
`render(T, float, PoseStack, MultiBufferSource, int, int)`), que é a forma
que tenho certeza real de como funciona - mas é o arquivo com mais chance
de precisar de ajuste. Se o Gradle reclamar de algo nesse arquivo
especificamente, me manda o erro que eu conserto na hora (assim como fizemos
com o mod anterior).

Todo o resto (bloco, block entity, logica de conserto, geometria do
modelo, resources) eu verifiquei contra a documentacao oficial mais recente
que consegui encontrar.

## Como funciona

### O bloco
- Clique direito com uma ferramenta/arma/elytra danificada (com durabilidade)
  → o item entra no bloco e fica "em exibição" sobre o suporte
  `display_item`.
- Clique direito com o material de reparo correto (o mesmo que a bigorna
  aceitaria - ferro para picareta de ferro, membrana de phantom para
  elytra, etc.) → adiciona material ao bloco.
- Enquanto tiver item danificado + material, o bloco entra no modo
  **refine**: repara 1 ponto de durabilidade a cada meio segundo, e a cada
  **50 pontos de durabilidade reparados** consome **apenas 1 unidade** do
  material - bem mais barato que a bigorna.
- Clique direito com a mão vazia → retira o item (consertado ou não) do
  bloco.
- Se o bloco for quebrado, o item e o material guardados caem no chão
  (nada se perde).

Esses números ficam em `RefineBlockEntity.java`:
```java
public static final int DURABILITY_PER_MATERIAL = 50; // barato de propósito
public static final int TICKS_PER_REPAIR_STEP = 10;    // velocidade do reparo
public static final int MAX_MATERIAL = 16;              // estoque máximo
```

### As animações
Como o Java (ao contrário do Bedrock) não tem um sistema de arquivo de
animação, as animações "idle" e "refine" são feitas por código, em
`RefineBlockEntityRenderer.applyAnimation()`:
- **idle**: quase parado, uma respiração bem sutil na manivela (`bone2`).
- **refine**: a manivela (`bone2`) gira continuamente feito uma manivela de
  verdade, e o torno (`rool`) balança de leve, como se estivesse
  prensando o item.

Ajuste a velocidade/intensidade direto nos multiplicadores dessa função.

### O modelo 3D
Convertido a mão do seu `.geo.json` (formato Bedrock do Blockbench) para o
sistema nativo de `ModelPart` do Java, em `RefineBlockModel.java`. Cada
"bone" virou uma parte; cubos com pivot/rotação próprios (diferentes do
bone) viraram sub-partes filhas, já que o Java só permite uma rotação por
parte, não por cubo individual.

**Se alguma peça aparecer girada para o lado errado no jogo** (é comum
acontecer ao converter rotações do formato Bedrock para o Java): abra
`RefineBlockModel.java`, ache o comentário `// ROTATION` na peça em
questão, e inverta o sinal do ângulo (ex.: troque `-45.0F` por `45.0F`).

O item exibido sobre o suporte usa uma escala fixa (`DISPLAY_ITEM_SCALE`
em `RefineBlockEntityRenderer.java`, hoje em `0.4F`) - ajuste esse número
se o item aparecer grande/pequeno demais.

## Como compilar

Pré-requisitos: **JDK 21**. O Gradle Wrapper já está incluso.

```bash
./gradlew build
```

O `.jar` aparece em `build/libs/refine-tool-1.0.0.jar`. Confira sempre a
versão do `fabric_api_version` em `gradle.properties` (compatível com
1.21.11) antes de compilar.

## Como instalar
1. Fabric Loader para 1.21.11.
2. Fabric API compatível com 1.21.11 na pasta `mods`.
3. `refine-tool-1.0.0.jar` também na pasta `mods`.

## Estrutura do projeto

```
refine-tool/
├── build.gradle / gradle.properties / settings.gradle
├── gradlew / gradlew.bat / gradle/wrapper/
├── .github/workflows/build.yml
└── src/main/
    ├── java/com/example/refinetool/
    │   ├── RefineToolMod.java              (entrypoint comum)
    │   ├── block/
    │   │   ├── RefineBlock.java            (bloco: forma, facing, interação)
    │   │   ├── ModBlocks.java
    │   │   ├── ModBlockEntities.java
    │   │   └── entity/RefineBlockEntity.java  (lógica de conserto)
    │   └── client/
    │       ├── RefineToolClient.java       (entrypoint client)
    │       ├── model/RefineBlockModel.java (geometria convertida do .geo.json)
    │       └── render/RefineBlockEntityRenderer.java (desenho + animações)
    └── resources/
        ├── fabric.mod.json
        ├── assets/refinetool/
        │   ├── textures/block/refine_tool.png
        │   ├── textures/item/refine_block.png
        │   ├── blockstates/refine_block.json
        │   ├── models/block/refine_block.json  (vazio - BER desenha tudo)
        │   ├── models/item/refine_block.json
        │   └── lang/en_us.json, pt_br.json
        └── data/refinetool/loot_table/blocks/refine_block.json
```

## Possíveis melhorias futuras
- GUI própria (hoje a interação é só clique direito, sem inventário visual).
- Partículas/som durante o refino.
- Bonemeal-like: mostrar barra de progresso flutuante sobre o bloco.
