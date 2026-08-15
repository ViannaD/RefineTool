package com.example.refinetool.client.render;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;

/**
 * A partir da 1.21.9, BlockEntityRenderer passou a usar um sistema de
 * "render state": os dados sao extraidos do BlockEntity em
 * extractRenderState() e so entao usados em submit() (que roda depois,
 * potencialmente em outro momento do frame). Isso evita acessar o
 * BlockEntity fora da thread/momento correto.
 */
public class RefineBlockEntityRenderState extends BlockEntityRenderState {
	public Direction facing = Direction.NORTH;
	public boolean refining = false;
	public float animationTime = 0.0F;
}
