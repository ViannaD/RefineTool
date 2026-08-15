package com.example.refinetool.client.model;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import com.example.refinetool.RefineToolMod;

/**
 * Modelo convertido a mao a partir de refine_tool.geo.json (formato Bedrock
 * do Blockbench). Cada "bone" do arquivo virou um PartDefinition; cubos com
 * pivot/rotation proprios (diferentes do bone) viraram sub-partes filhas,
 * ja que o ModelPart do Java so suporta rotacao por parte, nao por cubo.
 *
 * Hierarquia original:
 *   rool           (corpo/torno principal, com uma cunha rotacionada -45 no X)
 *   display_item   (apenas um "localizador" - usado para posicionar o item
 *                    sendo consertado, nao e renderizado como geometria)
 *   base           (plataforma + 4 pernas inclinadas +-17.5 no X)
 *     bone2        (manivela: braco -22.5 no X + cabo -45 no X)
 *
 * IMPORTANTE SOBRE ROTACOES: o formato Bedrock as vezes usa a direcao de
 * rotacao invertida em relacao ao formato "Java Block/Entity" nativo do
 * Blockbench. Os valores abaixo foram convertidos diretamente (sem inverter
 * sinal). Se alguma peca aparecer girada para o lado errado no jogo, troque
 * o sinal do angulo correspondente (ex.: -45 -> 45) na linha comentada com
 * "ROTATION" logo abaixo do PartDefinition daquele cubo.
 */
public final class RefineBlockModel {

	public static final ModelLayerLocation LAYER_LOCATION =
			new ModelLayerLocation(Identifier.fromNamespaceAndPath(RefineToolMod.MOD_ID, "refine_block"), "main");

	private final ModelPart root;
	public final ModelPart rool;
	public final ModelPart base;
	public final ModelPart bone2;
	public final ModelPart bone2Arm;
	public final ModelPart bone2Hand;
	public final ModelPart displayItem;

	public RefineBlockModel(ModelPart root) {
		this.root = root;
		this.rool = root.getChild("rool");
		this.base = root.getChild("base");
		this.bone2 = this.base.getChild("bone2");
		this.bone2Arm = this.bone2.getChild("bone2_arm");
		this.bone2Hand = this.bone2.getChild("bone2_hand");
		this.displayItem = root.getChild("display_item");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition rootPart = mesh.getRoot();

		// ---- rool (pivot -0.25, 11, 0) ----
		PartDefinition rool = rootPart.addOrReplaceChild("rool",
				CubeListBuilder.create()
						.texOffs(0, 51).addBox(-2.25F, -4.0F, -4.0F, 4.5F, 8.0F, 8.0F)
						.texOffs(42, 63).addBox(-2.25F, -3.0F, -5.0F, 4.5F, 6.0F, 1.0F)
						.texOffs(66, 14).addBox(-2.25F, -3.0F, 4.0F, 4.5F, 6.0F, 1.0F)
						.texOffs(48, 0).addBox(-2.25F, 4.0F, -3.0F, 4.5F, 1.0F, 6.0F)
						.texOffs(48, 7).addBox(-2.25F, -5.0F, -3.0F, 4.5F, 1.0F, 6.0F),
				PartPose.offset(-0.25F, 11.0F, 0.0F));

		// Cunha do torno - pivot igual ao do bone "rool", por isso offset (0,0,0).
		rool.addOrReplaceChild("rool_wedge",
				CubeListBuilder.create()
						.texOffs(24, 53).addBox(-2.75F, -2.0F, -2.0F, 5.5F, 4.0F, 4.0F),
				PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F,
						Mth.DEG_TO_RAD * -45.0F, 0.0F, 0.0F)); // ROTATION

		// ---- display_item (pivot -0.25, 16, -3) - so um localizador, sem geometria visivel ----
		rootPart.addOrReplaceChild("display_item",
				CubeListBuilder.create(),
				PartPose.offset(-0.25F, 16.0F, -3.0F));

		// ---- base (pivot 0, 0, 0) ----
		PartDefinition base = rootPart.addOrReplaceChild("base",
				CubeListBuilder.create()
						.texOffs(0, 0).addBox(-5.0F, 1.0F, -7.25F, 10.0F, 1.0F, 14.5F)
						.texOffs(0, 15).addBox(4.0F, 9.0F, -8.0F, 1.5F, 2.0F, 16.0F)
						.texOffs(52, 63).addBox(3.5F, 9.75F, -1.25F, 1.0F, 2.5F, 2.5F)
						.texOffs(0, 33).addBox(-5.5F, 9.0F, -8.0F, 1.5F, 2.0F, 16.0F)
						.texOffs(66, 21).addBox(2.0F, 10.0F, -1.0F, 3.0F, 2.0F, 2.0F)
						.texOffs(66, 25).addBox(-5.0F, 10.0F, -1.0F, 3.0F, 2.0F, 2.0F)
						.texOffs(60, 53).addBox(-4.0F, 9.0F, -7.0F, 8.0F, 2.0F, 1.0F)
						.texOffs(60, 56).addBox(-4.0F, 9.0F, 6.0F, 8.0F, 2.0F, 1.0F)
						.texOffs(34, 15).addBox(4.0F, 2.0F, -7.5F, 1.0F, 4.0F, 15.0F)
						.texOffs(0, 74).addBox(-4.0F, 2.0F, -6.0F, 8.0F, 3.0F, 12.0F)
						.texOffs(34, 34).addBox(-5.0F, 2.0F, -7.5F, 1.0F, 4.0F, 15.0F)
						.texOffs(42, 53).addBox(-4.0F, 2.0F, -7.0F, 8.0F, 4.0F, 1.0F)
						.texOffs(42, 58).addBox(-4.0F, 2.0F, 6.0F, 8.0F, 4.0F, 1.0F)
						.texOffs(66, 29).addBox(5.0F, 0.0F, -8.1F, 1.0F, 1.0F, 2.2F)
						.texOffs(66, 32).addBox(5.0F, 0.0F, 5.9F, 1.0F, 1.0F, 2.2F)
						.texOffs(66, 35).addBox(-6.0F, 0.0F, -8.1F, 1.0F, 1.0F, 2.2F)
						.texOffs(66, 38).addBox(-6.0F, 0.0F, 5.9F, 1.0F, 1.0F, 2.2F),
				PartPose.ZERO);

		CubeDeformation legInflate = new CubeDeformation(-0.001F);

		base.addOrReplaceChild("base_leg_front_right",
				CubeListBuilder.create()
						.texOffs(60, 59).addBox(-0.5F, -1.25F, -1.0F, 1.0F, 11.25F, 2.25F, legInflate),
				PartPose.offsetAndRotation(5.5F, 1.75F, -6.73579F,
						Mth.DEG_TO_RAD * -17.5F, 0.0F, 0.0F)); // ROTATION

		base.addOrReplaceChild("base_leg_back_right",
				CubeListBuilder.create()
						.texOffs(24, 61).addBox(-0.5F, -1.25F, -1.25F, 1.0F, 11.25F, 2.25F, legInflate),
				PartPose.offsetAndRotation(5.5F, 1.75F, 6.73579F,
						Mth.DEG_TO_RAD * 17.5F, 0.0F, 0.0F)); // ROTATION

		base.addOrReplaceChild("base_leg_front_left",
				CubeListBuilder.create()
						.texOffs(30, 61).addBox(-0.5F, -1.25F, -1.0F, 1.0F, 11.25F, 2.25F, legInflate),
				PartPose.offsetAndRotation(-5.5F, 1.75F, -6.73579F,
						Mth.DEG_TO_RAD * -17.5F, 0.0F, 0.0F)); // ROTATION

		base.addOrReplaceChild("base_leg_back_left",
				CubeListBuilder.create()
						.texOffs(36, 61).addBox(-0.5F, -1.25F, -1.25F, 1.0F, 11.25F, 2.25F, legInflate),
				PartPose.offsetAndRotation(-5.5F, 1.75F, 6.73579F,
						Mth.DEG_TO_RAD * 17.5F, 0.0F, 0.0F)); // ROTATION

		// ---- bone2 (manivela), filha de base, pivot (5.5, 11.5, 0) ----
		PartDefinition bone2 = base.addOrReplaceChild("bone2",
				CubeListBuilder.create(),
				PartPose.offset(5.5F, 11.5F, 0.0F));

		bone2.addOrReplaceChild("bone2_arm",
				CubeListBuilder.create()
						.texOffs(66, 41).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 4.0F, 1.0F),
				PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F,
						Mth.DEG_TO_RAD * -22.5F, 0.0F, 0.0F)); // ROTATION

		bone2.addOrReplaceChild("bone2_hand",
				CubeListBuilder.create()
						.texOffs(24, 51).addBox(-1.5F, -0.5F, -0.5F, 2.0F, 1.0F, 1.0F),
				PartPose.offsetAndRotation(2.0F, 2.77164F, 1.14805F,
						Mth.DEG_TO_RAD * -45.0F, 0.0F, 0.0F)); // ROTATION

		return LayerDefinition.create(mesh, 128, 128);
	}

	public ModelPart root() {
		return this.root;
	}
}
