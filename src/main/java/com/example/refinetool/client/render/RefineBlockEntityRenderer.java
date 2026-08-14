package com.example.refinetool.client.render;

import com.example.refinetool.RefineToolMod;
import com.example.refinetool.block.RefineBlock;
import com.example.refinetool.block.entity.RefineBlockEntity;
import com.example.refinetool.client.model.RefineBlockModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import org.joml.Quaternionf;
import org.joml.AxisAngle4f;

/**
 * Renderiza o modelo customizado do bloco de refino, anima "rool" (o torno)
 * e "bone2" (a manivela) e desenha o item guardado sobre o suporte
 * "display_item".
 *
 * ATENCAO - 1.21.11 e uma versao muito recente (posterior ao meu
 * conhecimento confiavel de treinamento) e a pipeline de renderizacao de
 * block entities mudou bastante entre a 1.21.1 e a 1.21.9/1.21.11 (novo
 * padrao BlockEntityRenderer<T, RenderState> com SubmitNodeCollector no
 * lugar de MultiBufferSource em alguns casos, e ItemStackRenderState para
 * itens). Este arquivo usa a API "classica" (BlockEntityRenderer<T> com
 * render(T, float, PoseStack, MultiBufferSource, int, int)), que e a forma
 * que eu tenho certeza real de como funciona. Se o Gradle reclamar que essa
 * interface/metodo nao existe mais ou pede tipos diferentes, esse e o
 * arquivo mais provavel de precisar de ajuste - veja:
 * https://docs.neoforged.net/primer/docs/1.21.9/ (secao sobre
 * BlockEntityRenderer / ItemStackRenderState) para o padrao novo.
 */
public class RefineBlockEntityRenderer implements BlockEntityRenderer<RefineBlockEntity> {

	private static final Identifier TEXTURE =
			Identifier.fromNamespaceAndPath(RefineToolMod.MOD_ID, "textures/block/refine_tool.png");

	/** Ajuste este valor para mudar o tamanho do item exibido sobre o suporte. */
	private static final float DISPLAY_ITEM_SCALE = 0.4F;

	private final RefineBlockModel model;
	private final ItemRenderer itemRenderer;

	public RefineBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
		this.model = new RefineBlockModel(context.bakeLayer(RefineBlockModel.LAYER_LOCATION));
		this.itemRenderer = context.getItemRenderer();
	}

	@Override
	public void render(RefineBlockEntity blockEntity, float partialTick, PoseStack poseStack,
			MultiBufferSource buffer, int packedLight, int packedOverlay) {

		long gameTime = blockEntity.getLevel() != null ? blockEntity.getLevel().getGameTime() : 0L;
		float animTime = gameTime + partialTick;

		applyAnimation(blockEntity.isRefining(), animTime);

		Direction facing = blockEntity.getBlockState().getValue(RefineBlock.FACING);
		float yRotDegrees = 180.0F - facing.toYRot();

		poseStack.pushPose();
		poseStack.translate(0.5D, 0.0D, 0.5D);
		poseStack.mulPose(new Quaternionf(new AxisAngle4f(Mth.DEG_TO_RAD * yRotDegrees, 0.0F, 1.0F, 0.0F)));

		var vertexConsumer = buffer.getBuffer(RenderType.entityCutout(TEXTURE));
		this.model.root().render(poseStack, vertexConsumer, packedLight, packedOverlay);

		ItemStack repairItem = blockEntity.getRepairItem();
		if (!repairItem.isEmpty()) {
			poseStack.pushPose();
			this.model.displayItem.translateAndRotate(poseStack);
			poseStack.scale(DISPLAY_ITEM_SCALE, DISPLAY_ITEM_SCALE, DISPLAY_ITEM_SCALE);
			this.itemRenderer.renderStatic(repairItem, ItemDisplayContext.FIXED, packedLight, packedOverlay,
					poseStack, buffer, blockEntity.getLevel(), 0);
			poseStack.popPose();
		}

		poseStack.popPose();
	}

	/**
	 * Animacoes puramente proceduais (sem arquivo de animation.json - o Java
	 * vanilla nao tem um sistema de animacao por arquivo como o Bedrock).
	 * "idle" = bloco parado, "refine" = bloco consertando o item.
	 */
	private void applyAnimation(boolean refining, float animTime) {
		if (refining) {
			// Manivela (bone2) girando continuamente.
			float crank = animTime * 0.35F;
			this.model.bone2Arm.xRot = crank;
			this.model.bone2Hand.xRot = crank;
			// Torno (rool) balancando de leve, como se estivesse prensando o item.
			this.model.rool.xRot = Mth.sin(animTime * 0.5F) * 0.12F;
		} else {
			// Idle: respiracao bem sutil, quase parado.
			float idle = Mth.sin(animTime * 0.05F);
			this.model.bone2Arm.xRot = idle * 0.03F;
			this.model.bone2Hand.xRot = idle * 0.03F;
			this.model.rool.xRot = 0.0F;
		}
	}
}
