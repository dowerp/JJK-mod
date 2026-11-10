package com.dowerp.sukuna.client;

import com.dowerp.sukuna.SukunaMod;
import com.dowerp.sukuna.entity.SpriteFxEntity;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

/**
 * Рисует эффекты техник как объёмные 3D-меши (многогранные трубки, торы, лезвия, шипы), собранные из кубоидов.
 * Единица модели = 16 пикселей = 1 блок; длина и ширина эффекта задаются масштабом при отрисовке.
 */
public class SpriteFxRenderer extends EntityRenderer<SpriteFxEntity, SpriteFxRenderer.SpriteState> {
    private static final int FULL_LIGHT = 15728880;
    private static final int KIND_COUNT = 10;
    private static final Identifier[] TEXTURES = new Identifier[5];

    static {
        for (int p = 0; p < 5; p++) {
            TEXTURES[p] = SukunaMod.id("textures/entity/fx3d/pal_" + p + ".png");
        }
    }

    private final MeshModel[] models = new MeshModel[KIND_COUNT];

    public SpriteFxRenderer(EntityRendererFactory.Context context) {
        super(context);
        for (int k = 0; k < KIND_COUNT; k++) {
            this.models[k] = new MeshModel(buildMesh(k).createModel());
        }
    }

    public static class SpriteState extends EntityRenderState {
        public int kind;
        public int palette;
        public float length;
        public float width;
        public float roll;
        public float yaw;
        public float pitch;
        public float lifeFrac;
    }

    private static final class MeshModel extends Model<SpriteState> {
        MeshModel(ModelPart root) {
            super(root, RenderLayers::entityCutoutNoCull);
        }
    }

    // ------------------------------------------------------------------ геометрия
    private static final class Mesh {
        final ModelData data = new ModelData();
        final ModelPartData root = data.getRoot();
        int n = 0;

        /** Куб с центром в (px,py,pz) и поворотом (rx,ry,rz) в радианах. Размеры в пикселях (16 = 1 блок). */
        void box(float sx, float sy, float sz, float px, float py, float pz, float rx, float ry, float rz) {
            root.addChild("b" + (n++), ModelPartBuilder.create().uv(0, 0).cuboid(-sx / 2.0F, -sy / 2.0F, -sz / 2.0F, sx, sy, sz), ModelTransform.of(px, py, pz, rx, ry, rz));
        }

        void box(float sx, float sy, float sz, float px, float py, float pz) {
            box(sx, sy, sz, px, py, pz, 0.0F, 0.0F, 0.0F);
        }

        /** Восьмигранное сечение: два куба, один повёрнут на 45 градусов вокруг оси X. */
        void octo(float sx, float t, float px) {
            box(sx, t, t, px, 0.0F, 0.0F);
            box(sx, t, t, px, 0.0F, 0.0F, (float) (Math.PI / 4.0), 0.0F, 0.0F);
        }

        TexturedModelData done() {
            return TexturedModelData.of(data, 16, 16);
        }
    }

    private static final float PI = (float) Math.PI;

    private static TexturedModelData buildMesh(int kind) {
        Mesh m = new Mesh();
        switch (kind) {
            case 0 -> { // STREAK: огранённый луч с заострёнными концами
                m.octo(8.0F, 7.0F, 0.0F);
                m.octo(3.6F, 5.0F, -5.4F);
                m.octo(3.6F, 5.0F, 5.4F);
                m.octo(2.2F, 2.6F, -7.6F);
                m.octo(2.2F, 2.6F, 7.6F);
            }
            case 1 -> { // CRESCENT: изогнутое лезвие с утолщением в центре и внутренним слоем
                int n = 11;
                for (int i = 0; i < n; i++) {
                    float a = (-1.0F + 2.0F * i / (n - 1)) * 1.08F;
                    float thick = 0.9F + 3.8F * MathHelper.cos(a * 1.35F);
                    m.box(3.6F, 2.2F, thick, 7.6F * MathHelper.sin(a), 0.0F, 7.4F * MathHelper.cos(a) - 3.4F, 0.0F, a, 0.0F);
                    m.box(3.4F, 1.2F, thick * 0.55F, 6.2F * MathHelper.sin(a), 1.2F, 6.2F * MathHelper.cos(a) - 3.4F, 0.0F, a, 0.0F);
                }
            }
            case 2 -> { // BOLT: зигзаг из скрещённых звеньев
                for (int i = 0; i < 7; i++) {
                    float x = -7.2F + i * 2.4F;
                    float dy = (i % 2 == 0 ? 1.0F : -1.0F) * 1.5F;
                    float rz = (i % 2 == 0 ? 1.0F : -1.0F) * 0.55F;
                    m.box(3.4F, 2.6F, 2.6F, x, dy, 0.0F, 0.0F, 0.0F, rz);
                    m.box(3.4F, 2.6F, 2.6F, x, dy, 0.0F, PI / 2.0F, 0.0F, rz);
                }
                m.box(16.0F, 1.0F, 1.0F, 0.0F, 0.0F, 0.0F);
            }
            case 3 -> { // RING: тор из 16 звеньев плюс два тонких внутренних кольца
                ringXY(m, 16, 7.2F, 3.3F, 1.3F, 1.6F, 0.0F);
                ringXY(m, 16, 6.2F, 3.0F, 0.8F, 0.8F, 1.1F);
                ringXY(m, 16, 6.2F, 3.0F, 0.8F, 0.8F, -1.1F);
            }
            case 4 -> { // BURST: шипы по всем осям и диагоналям плюс огранённое ядро
                float[] r = {3.2F, 5.6F, 7.4F};
                float[] len = {3.6F, 3.0F, 2.2F};
                float[] th = {2.6F, 1.6F, 0.8F};
                for (int i = 0; i < 3; i++) {
                    for (int s = -1; s <= 1; s += 2) {
                        m.box(len[i], th[i], th[i], s * r[i], 0.0F, 0.0F);
                        m.box(th[i], len[i], th[i], 0.0F, s * r[i], 0.0F);
                        m.box(th[i], th[i], len[i], 0.0F, 0.0F, s * r[i]);
                        float d = r[i] * 0.7071F;
                        m.box(len[i], th[i], th[i], s * d, d, 0.0F, 0.0F, 0.0F, s * PI / 4.0F);
                        m.box(len[i], th[i], th[i], s * d, -d, 0.0F, 0.0F, 0.0F, -s * PI / 4.0F);
                    }
                }
                m.box(4.2F, 4.2F, 4.2F, 0.0F, 0.0F, 0.0F, PI / 4.0F, PI / 4.0F, 0.0F);
                m.box(3.0F, 3.0F, 3.0F, 0.0F, 0.0F, 0.0F);
            }
            case 5 -> { // DISTORT: гироскоп из трёх перпендикулярных колец
                int n = 12;
                for (int i = 0; i < n; i++) {
                    float a = (float) (2.0 * Math.PI * i / n);
                    float c = 7.0F * MathHelper.cos(a);
                    float s = 7.0F * MathHelper.sin(a);
                    m.box(3.8F, 1.0F, 1.0F, c, s, 0.0F, 0.0F, 0.0F, a + PI / 2.0F);
                    m.box(3.8F, 1.0F, 1.0F, c, 0.0F, s, 0.0F, -(a + PI / 2.0F), 0.0F);
                    m.box(1.0F, 1.0F, 3.8F, 0.0F, c, s, a, 0.0F, 0.0F);
                }
            }
            case 6 -> { // BLOB: гранёный комок с выступами
                m.box(7.0F, 7.0F, 7.0F, 0.0F, 0.0F, 0.0F);
                m.box(7.0F, 7.0F, 7.0F, 0.0F, 0.0F, 0.0F, PI / 4.0F, 0.0F, 0.0F);
                m.box(7.0F, 7.0F, 7.0F, 0.0F, 0.0F, 0.0F, 0.0F, PI / 4.0F, 0.0F);
                m.box(7.0F, 7.0F, 7.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, PI / 4.0F);
                m.box(3.0F, 3.0F, 3.0F, 4.0F, 3.0F, 2.0F, 0.3F, 0.4F, 0.2F);
                m.box(2.6F, 2.6F, 2.6F, -3.5F, 4.0F, -3.0F, 0.5F, 0.1F, 0.3F);
                m.box(2.8F, 2.8F, 2.8F, 2.0F, -4.0F, 4.0F, 0.2F, 0.6F, 0.1F);
            }
            case 7 -> { // SPLASH: плоская корона из шипов с каплями
                m.box(9.0F, 9.0F, 1.4F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, PI / 8.0F);
                for (int i = 0; i < 8; i++) {
                    float a = i * PI / 4.0F;
                    m.box(5.2F, 1.8F, 1.4F, 7.0F * MathHelper.cos(a), 7.0F * MathHelper.sin(a), 0.2F, 0.0F, 0.0F, a);
                }
                for (int sx = -1; sx <= 1; sx += 2) {
                    for (int sy = -1; sy <= 1; sy += 2) {
                        m.box(2.2F, 2.2F, 2.2F, sx * 3.6F, sy * 3.6F, 2.6F, 0.4F, 0.4F, 0.0F);
                    }
                }
            }
            case 8 -> { // TORRENT: толстый поток с утолщениями
                m.octo(10.0F, 8.0F, 0.0F);
                m.octo(4.0F, 5.0F, -6.2F);
                m.octo(4.0F, 5.0F, 6.2F);
                for (int i = -1; i <= 1; i++) {
                    m.box(9.0F, 9.0F, 9.0F, i * 4.5F, 0.0F, 0.0F, PI / 4.0F, PI / 4.0F, 0.0F);
                }
            }
            default -> { // ARROW (9): огненная стрела с наконечником и оперением
                m.octo(10.0F, 3.6F, -2.0F);
                m.octo(3.0F, 6.4F, 4.2F);
                m.octo(2.4F, 4.2F, 6.2F);
                m.octo(1.6F, 2.2F, 7.6F);
                m.box(3.0F, 8.0F, 0.8F, -7.0F, 0.0F, 0.0F);
                m.box(3.0F, 0.8F, 8.0F, -7.0F, 0.0F, 0.0F);
                m.octo(3.0F, 5.0F, -8.0F);
            }
        }
        return m.done();
    }

    private static void ringXY(Mesh m, int n, float radius, float tangent, float radial, float depth, float z) {
        for (int i = 0; i < n; i++) {
            float a = (float) (2.0 * Math.PI * i / n);
            m.box(tangent, radial, depth, radius * MathHelper.cos(a), radius * MathHelper.sin(a), z, 0.0F, 0.0F, a + PI / 2.0F);
        }
    }

    // ------------------------------------------------------------------ состояние и отрисовка
    @Override
    public SpriteState createRenderState() {
        return new SpriteState();
    }

    @Override
    public void updateRenderState(SpriteFxEntity e, SpriteState s, float tickDelta) {
        super.updateRenderState(e, s, tickDelta);
        s.kind = MathHelper.clamp(e.kind(), 0, KIND_COUNT - 1);
        s.palette = s.kind == 5 ? 0 : s.kind == 9 ? 4 : s.kind >= 6 ? 1 : MathHelper.clamp(e.palette(), 0, 4);
        s.length = e.length();
        s.width = e.width();
        s.roll = e.roll();
        s.yaw = e.getYaw();
        s.pitch = e.getPitch();
        s.lifeFrac = Math.min(1.0F, (e.age + tickDelta) / e.life());
    }

    @Override
    public boolean shouldRender(SpriteFxEntity entity, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public void render(SpriteState s, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState camera) {
        float fade = Math.min(1.0F, (1.0F - s.lifeFrac) / 0.35F);
        float pop = 1.0F + 0.12F * s.lifeFrac;
        float len = s.length * pop * Math.max(0.05F, fade);
        float wid = s.width * pop * Math.max(0.05F, fade);
        float spin = (s.kind == 4 || s.kind == 5 || s.kind == 6) ? s.lifeFrac * 50.0F : 0.0F;

        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-s.yaw));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(s.pitch));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(s.roll + spin));

        switch (s.kind) {
            case 0, 2, 8 -> { // вдоль направления, ось X модели -> вперёд
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-90.0F));
                matrices.scale(len, wid, wid);
            }
            case 9 -> { // стрела: остриё вперёд
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-90.0F));
                matrices.scale(len, wid, wid);
            }
            case 1 -> matrices.scale(len, wid * 0.22F, len * 0.9F);
            case 3 -> matrices.scale(len, wid, (len + wid) * 0.25F);
            case 7 -> matrices.scale(len, wid, (len + wid) * 0.3F);
            default -> matrices.scale(len, wid, (len + wid) * 0.5F);
        }
        queue.submitModel(models[s.kind], s, matrices, RenderLayers.entityTranslucentEmissive(TEXTURES[s.palette]), FULL_LIGHT, OverlayTexture.DEFAULT_UV, 0, null);
        matrices.pop();
    }
}
