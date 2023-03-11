package io.github.lightman314.lightmanscurrency.client.gui.widget.button.icon;

import java.util.List;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;

import io.github.lightman314.lightmanscurrency.client.util.ItemRenderUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public abstract class IconData {

    @Environment(EnvType.CLIENT)
    public abstract void render(MatrixStack pose, ClickableWidget widget, TextRenderer font, int x, int y);

    private static class ItemIcon extends IconData
    {
        private final ItemStack iconStack;
        private ItemIcon(ItemStack iconStack) { this.iconStack = iconStack; }

        @Override
        public void render(MatrixStack matrixStack, ClickableWidget widget, TextRenderer font, int x, int y)
        {
            ItemRenderUtil.drawItemStack(widget, font, this.iconStack, x, y);
        }

    }

    private static class ImageIcon extends IconData
    {
        private final Identifier iconImage;
        private final int iconImageU;
        private final int iconImageV;
        private ImageIcon(Identifier iconImage, int u, int v) {
            this.iconImage = iconImage;
            this.iconImageU = u;
            this.iconImageV = v;
        }

        @Override
        public void render(MatrixStack matrixStack, ClickableWidget widget, TextRenderer font, int x, int y)
        {
            RenderSystem.setShaderTexture(0, this.iconImage);
            widget.drawTexture(matrixStack, x, y, iconImageU, iconImageV, 16, 16);
        }

    }

    private static class TextIcon extends IconData
    {
        private final Text iconText;
        private final int textColor;
        private TextIcon(Text iconText, int textColor) {
            this.iconText = iconText;
            this.textColor = textColor;
        }

        @Override
        public void render(MatrixStack matrixStack, ClickableWidget widget, TextRenderer font, int x, int y)
        {
            int xPos = x + 8 - (font.getWidth(iconText.getString())/2);
            int yPos = y + ((16 - font.fontHeight) / 2);
            font.drawWithShadow(matrixStack, this.iconText.getString(), xPos, yPos, this.textColor);
        }
    }

    private static class MultiIcon extends IconData
    {
        private final List<IconData> icons;
        private MultiIcon(List<IconData> icons) { this.icons = icons; }
        @Override
        public void render(MatrixStack pose, ClickableWidget widget, TextRenderer font, int x, int y) {
            for(IconData icon : this.icons)
                icon.render(pose, widget, font, x, y);
        }
    }

    public static final IconData BLANK = new IconData() { public void render(MatrixStack pose, ClickableWidget widget, TextRenderer font, int x, int y) {} };

    public static IconData of(ItemConvertible item) { return of(new ItemStack(item)); }
    public static IconData of(ItemStack iconStack) { return new ItemIcon(iconStack); }
    public static IconData of(Identifier iconImage, int u, int v) { return new ImageIcon(iconImage, u,v); }
    public static IconData of(Text iconText) { return new TextIcon(iconText, 0xFFFFFF); }
    public static IconData of(Text iconText, int textColor) { return new TextIcon(iconText, textColor); }
    public static IconData of(IconData... icons) { return new MultiIcon(Lists.newArrayList(icons)); }

}