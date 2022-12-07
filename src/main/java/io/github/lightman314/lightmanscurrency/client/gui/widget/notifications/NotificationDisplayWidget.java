package io.github.lightman314.lightmanscurrency.client.gui.widget.notifications;

import java.util.List;
import java.util.function.Supplier;

import com.mojang.blaze3d.systems.RenderSystem;

import io.github.lightman314.lightmanscurrency.client.gui.widget.ScrollBarWidget.IScrollable;
import io.github.lightman314.lightmanscurrency.common.LightmansCurrency;
import io.github.lightman314.lightmanscurrency.common.notifications.Notification;
import io.github.lightman314.lightmanscurrency.util.MathUtil;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class NotificationDisplayWidget extends ClickableWidget implements IScrollable {

    public static final Identifier GUI_TEXTURE =  new Identifier(LightmansCurrency.MODID, "textures/gui/notifications.png");

    public static final int HEIGHT_PER_ROW = 22;

    private final Supplier<List<Notification>> notificationSource;
    private final TextRenderer font;
    private final int rowCount;
    public boolean colorIfUnseen = false;
    public int backgroundColor = 0xFFC6C6C6;

    public static final int CalculateHeight(int rowCount) { return rowCount * HEIGHT_PER_ROW; }

    private List<Notification> getNotifications() { return this.notificationSource.get(); }

    Text tooltip = null;

    public NotificationDisplayWidget(int x, int y, int width, int rowCount, TextRenderer font, Supplier<List<Notification>> notificationSource) {
        super(x, y, width, CalculateHeight(rowCount), Text.empty());
        this.notificationSource = notificationSource;
        this.font = font;
        this.rowCount = rowCount;
    }

    @Override
    public void renderButton(MatrixStack pose, int mouseX, int mouseY, float partialTicks) {
        this.validateScroll();

        this.tooltip = null;

        List<Notification> notifications = this.getNotifications();
        int index = this.scroll;

        Screen.fill(pose, this.x, this.y, this.x + this.width, this.y + this.height, this.backgroundColor);

        for(int y = 0; y < this.rowCount && index < notifications.size(); ++y)
        {
            int yPos = this.y + y * HEIGHT_PER_ROW;
            Notification n = notifications.get(index++);

            //Draw the background
            RenderSystem.setShaderTexture(0, GUI_TEXTURE);
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            int vPos = n.wasSeen() && this.colorIfUnseen ? 222 : 200;
            this.drawTexture(pose, this.x, yPos, 0, vPos, 2, HEIGHT_PER_ROW);
            int xPos = this.x + 2;
            while(xPos < this.x + this.width - 2)
            {
                int thisWidth = Math.min(166, this.x + this.width - 2 - xPos);
                this.drawTexture(pose, xPos, yPos, 2, vPos, thisWidth, HEIGHT_PER_ROW);
                xPos += thisWidth;
            }
            this.drawTexture(pose, this.x + this.width - 2, yPos, 168, 200, 2, HEIGHT_PER_ROW);

            //Draw the text
            int textXPos = this.x + 2;
            int textWidth = this.width - 4;
            int textColor = n.wasSeen() ? 0xFFFFFF : 0x000000;
            if(n.getCount() > 1)
            {
                //Render quantity text
                String countText = String.valueOf(n.getCount());
                int quantityWidth = this.font.getWidth(countText);
                this.drawTexture(pose, this.x + 1 + quantityWidth, yPos, 170, vPos, 3, HEIGHT_PER_ROW);

                this.font.draw(pose, countText, textXPos, yPos + (HEIGHT_PER_ROW / 2) - (this.font.fontHeight / 2), textColor);

                textXPos += quantityWidth + 2;
                textWidth -= quantityWidth + 2;
            }

            Text message = n.getMessage();
            List<OrderedText> lines = this.font.wrapLines(message, textWidth);
            if(lines.size() == 1)
            {
                this.font.draw(pose, lines.get(0), textXPos, yPos + (HEIGHT_PER_ROW / 2) - (this.font.fontHeight / 2), textColor);
            }
            else
            {
                for(int l = 0; l < lines.size() && l < 2; ++l)
                    this.font.draw(pose, lines.get(l), textXPos, yPos + 2 + l * 10, textColor);
                if(lines.size() > 2 && this.tooltip == null && mouseX >= this.x && mouseX < this.x + this.width && mouseY >= yPos && mouseY < yPos + HEIGHT_PER_ROW)
                    this.tooltip = message;
            }

        }

    }

    public void tryRenderTooltip(MatrixStack pose, Screen screen, int mouseX, int mouseY)
    {
        if(this.tooltip != null)
        {
            screen.renderOrderedTooltip(pose, this.font.wrapLines(this.tooltip, this.width), mouseX, mouseY);
            this.tooltip = null;
        }
    }

    private int scroll = 0;

    @Override
    public int currentScroll() { return this.scroll; }

    @Override
    public void setScroll(int newScroll) {
        this.scroll = newScroll;
        this.validateScroll();
    }

    private void validateScroll() { this.scroll = MathUtil.clamp(this.scroll, 0, this.getMaxScroll()); }

    @Override
    public int getMaxScroll() { return Math.max(0, this.getNotifications().size() - this.rowCount); }

    @Override
    public void appendNarrations(NarrationMessageBuilder builder) { }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scroll) {
        if(scroll < 0)
            this.setScroll(this.scroll + 1);
        else if(scroll > 0)
            this.setScroll(this.scroll - 1);
        return true;
    }

    public void playDownSound(SoundManager manager) {}

}