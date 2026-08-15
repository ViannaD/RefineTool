package com.example.refinetool.client.render;

import com.example.refinetool.RefineToolMod;
import com.example.refinetool.block.RefineBlock;
import com.example.refinetool.block.entity.RefineBlockEntity;
import com.example.refinetool.client.model.RefineBlockModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

/**
 * Renderiza o modelo customizado do bloco de refino e anima "rool" (o torno)
 * e "bone2" (a manivela).
 *
 * NOTA SOBRE 1.21.11: a pipeline de renderizacao de block entities mudou
 * bastante entre a 1.21.1 e a 1.21.9/1.21.11 (novo padrao com dois
 * parametros de tipo - T = block entity, S = render state - e tres metodos:
 * createRenderState / extractRenderState / submit). Verifiquei a assinatura
 * exata de cada metodo usado aqui (incluindo submitModelPart) contra o
 * javadoc oficial da propria 1.21.11, entao a confianca aqui e alta.
 *
 * IMPORTANTE: removi temporariamente a renderizacao do ITEM sobre o suporte
 * "display_item" (essa parte dependeria de ItemStackRenderState/
 * ItemModelResolver, uma API bem mais nova e que nao consegui confirmar com
 * a mesma confianca). O bloco, a logica de conserto e a geometria/animacao
 * 3D funcionam normalmente; so falta essa parte visual extra. Veja o README
 * para como adicionar isso de volta.
 */
public class RefineBlockEntityRenderer implements BlockEntityRenderer<RefineBlockEntity, RefineBlockEntityRenderState> {

	private static final Identifier TEXTURE =
			Identifier.fromNamespaceAndPath(RefineToolMod.MOD_ID, "textures/block/refine_tool.png");

	private final RefineBlockModel model;

	public RefineBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
		this.model = new RefineBlockModel(context.bakeLayer(RefineBlockModel.LAYER_LOCATION));
	}

	@Override
	public RefineBlockEntityRenderState createRenderState() {
		return new RefineBlockEntityRenderState();
	}

	@Override
	public void extractRenderState(RefineBlockEntity blockEntity, RefineBlockEntityRenderState renderState,
			float partialTick, Vec3 cameraPos, @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
		BlockEntityRenderState.extractBase(blockEntity, renderState, crumblingOverlay);
		renderState.facing = blockEntity.getBlockState().getValue(RefineBlock.FACING);
		renderState.refining = blockEntity.isRefining();
		long gameTime = blockEntity.getLevel() != null ? blockEntity.getLevel().getGameTime() : 0L;
		renderState.animationTime = gameTime + partialTick;
	}

	@Override
	public void submit(RefineBlockEntityRenderState renderState, PoseStack poseStack, SubmitNodeCollector collector,
			CameraRenderState cameraState) {
		applyAnimation(renderState.refining, renderState.animationTime);

		float yRotDegrees = 180.0F - renderState.facing.toYRot();

		poseStack.pushPose();
		poseStack.translate(0.5D, 0.0D, 0.5D);
		poseStack.mulPose(new Quaternionf(new AxisAngle4f(Mth.DEG_TO_RAD * yRotDegrees, 0.0F, 1.0F, 0.0F)));

		// Assinatura confirmada no javadoc oficial da 1.21.11:
		// submitModelPart(ModelPart, PoseStack, RenderType, int light, int overlay, TextureAtlasSprite sprite)
		collector.submitModelPart(
				this.model.root(),
				poseStack,
				RenderType.entityCutout(TEXTURE),
				renderState.lightCoords,
				net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
				null
		);

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
