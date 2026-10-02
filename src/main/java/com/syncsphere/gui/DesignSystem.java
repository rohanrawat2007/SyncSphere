package com.syncsphere.gui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.AbstractBorder;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

/** Shared visual primitives for the Figma-derived Swing interface. */
public final class DesignSystem {
    public static final Color BACKGROUND = new Color(13, 17, 21);
    public static final Color SURFACE = new Color(26, 31, 36, 210);
    public static final Color SURFACE_RAISED = new Color(34, 40, 46, 230);
    public static final Color INPUT = new Color(23, 28, 32, 200);
    public static final Color PRIMARY = new Color(202, 165, 121);
    public static final Color ACCENT = new Color(128, 157, 149);
    public static final Color SUCCESS = new Color(122, 156, 128);
    public static final Color WARNING = new Color(191, 156, 110);
    public static final Color DANGER = new Color(164, 109, 101);
    public static final Color TEXT = new Color(243, 239, 233);
    public static final Color MUTED = new Color(172, 181, 184);
    public static final Color BORDER = new Color(255, 255, 255, 35);
    public static final Color FOCUS = new Color(202, 165, 121, 180);

    public static final Color LIGHT_BACKGROUND = new Color(245, 239, 232);
    public static final Color LIGHT_SURFACE = new Color(255, 255, 255, 220);
    public static final Color LIGHT_RAISED = new Color(255, 255, 255, 245);
    public static final Color LIGHT_INPUT = new Color(247, 241, 234);
    public static final Color LIGHT_TEXT = new Color(21, 27, 30);
    public static final Color LIGHT_MUTED = new Color(93, 104, 111);
    public static final Color LIGHT_BORDER = new Color(90, 96, 101, 30);

    private DesignSystem() { }

    public static Palette palette(boolean dark) {
        return dark
                ? new Palette(BACKGROUND, SURFACE, SURFACE_RAISED, INPUT, TEXT, MUTED, BORDER)
                : new Palette(LIGHT_BACKGROUND, LIGHT_SURFACE, LIGHT_RAISED, LIGHT_INPUT, LIGHT_TEXT, LIGHT_MUTED, LIGHT_BORDER);
    }

    public static Font font(int style, float size) { return new Font("Segoe UI", style, Math.round(size)); }

    public static void styleField(JTextField field) { styleField(field, true); }

    public static void styleField(JTextField field, boolean dark) {
        Palette colors = palette(dark);
        field.setFont(font(Font.PLAIN, 14));
        field.setBackground(colors.input());
        field.setForeground(colors.text());
        field.setCaretColor(colors.text());
        field.setSelectionColor(PRIMARY);
        field.setSelectedTextColor(TEXT);
        field.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(colors.border(), 12),
                BorderFactory.createEmptyBorder(11, 14, 11, 14)));
        field.putClientProperty("JComponent.sizeVariant", "regular");
    }

    public static void styleField(JPasswordField field, boolean dark) { styleField((JTextField) field, dark); }

    public static void styleButton(JButton button, Color background, Color foreground) {
        button.setFont(font(Font.BOLD, 13));
        button.setBackground(background);
        button.setForeground(foreground);
        button.setMargin(new Insets(8, 14, 8, 14));
        button.setMinimumSize(new Dimension(72, 36));
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setBorder(new RoundedBorder(new Color(255, 255, 255, 40), 12));
        button.setRolloverEnabled(true);
        button.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        button.setToolTipText(button.getText());
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            final Color original = background;
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { button.setBackground(brighten(original)); button.repaint(); }
            @Override public void mouseExited(java.awt.event.MouseEvent e) { button.setBackground(original); button.repaint(); }
            @Override public void mousePressed(java.awt.event.MouseEvent e) { button.setBackground(original.darker()); button.repaint(); }
            @Override public void mouseReleased(java.awt.event.MouseEvent e) { button.setBackground(original); button.repaint(); }
        });
    }

    public static void stylePanel(JComponent component, Color background) {
        component.setBackground(background);
        component.setBorder(BorderFactory.createCompoundBorder(new RoundedBorder(BORDER, 18),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)));
    }

    /** A panel that subtly highlights on mouse hover. */
    public static class HoverPanel extends JPanel {
        private boolean hovered = false;
        private final Color normalBg;
        private final Color hoverBg;

        public HoverPanel(Color normalBg, Color hoverBg) {
            this.normalBg = normalBg;
            this.hoverBg = hoverBg;
            setOpaque(true);
            setBackground(normalBg);
            setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseEntered(java.awt.event.MouseEvent e) { hovered = true; setBackground(hoverBg); repaint(); }
                @Override public void mouseExited(java.awt.event.MouseEvent e) { hovered = false; setBackground(normalBg); repaint(); }
            });
        }

        public boolean isHovered() { return hovered; }
    }

    /** Navigation rail button with active indicator bar. */
    public static class NavButton extends JButton {
        private boolean active = false;
        private final Color activeColor;

        public NavButton(String icon, Color activeColor) {
            super(icon);
            this.activeColor = activeColor;
            setFont(font(Font.PLAIN, 20));
            setForeground(MUTED);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setBorderPainted(false);
            setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(44, 44));
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseEntered(java.awt.event.MouseEvent e) { if (!active) { setForeground(TEXT); repaint(); } }
                @Override public void mouseExited(java.awt.event.MouseEvent e) { if (!active) { setForeground(MUTED); repaint(); } }
            });
        }

        public void setActive(boolean active) {
            this.active = active;
            setForeground(active ? activeColor : MUTED);
            repaint();
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (active) {
                g2.setColor(new Color(activeColor.getRed(), activeColor.getGreen(), activeColor.getBlue(), 30));
                g2.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 12, 12);
                g2.setColor(activeColor);
                g2.fillRoundRect(0, (getHeight() - 24) / 2, 3, 24, 3, 3);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Empty state panel with icon, title, subtitle. */
    public static class EmptyState extends JPanel {
        public EmptyState(String icon, String title, String subtitle, boolean dark) {
            setOpaque(false);
            setLayout(new java.awt.GridBagLayout());
            java.awt.GridBagConstraints gbc = new java.awt.GridBagConstraints();
            gbc.gridy = 0; gbc.gridx = 0; gbc.anchor = java.awt.GridBagConstraints.CENTER;
            gbc.insets = new Insets(0, 0, 8, 0);
            Palette colors = palette(dark);
            JLabel iconLabel = new JLabel(icon, SwingConstants.CENTER);
            iconLabel.setFont(font(Font.PLAIN, 38));
            iconLabel.setForeground(new Color(colors.muted().getRed(), colors.muted().getGreen(), colors.muted().getBlue(), 120));
            add(iconLabel, gbc);
            gbc.gridy = 1; gbc.insets = new Insets(0, 0, 4, 0);
            JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
            titleLabel.setFont(font(Font.BOLD, 15));
            titleLabel.setForeground(colors.muted());
            add(titleLabel, gbc);
            if (subtitle != null && !subtitle.isBlank()) {
                gbc.gridy = 2; gbc.insets = new Insets(0, 0, 0, 0);
                JLabel subLabel = new JLabel(subtitle, SwingConstants.CENTER);
                subLabel.setFont(font(Font.PLAIN, 12));
                subLabel.setForeground(new Color(colors.muted().getRed(), colors.muted().getGreen(), colors.muted().getBlue(), 160));
                add(subLabel, gbc);
            }
        }
    }

    /** Loading state panel. */
    public static class LoadingPanel extends JPanel {
        public LoadingPanel(String message, boolean dark) {
            setOpaque(false);
            setLayout(new java.awt.GridBagLayout());
            JLabel label = new JLabel("\u27F3  " + message, SwingConstants.CENTER);
            label.setFont(font(Font.PLAIN, 14));
            label.setForeground(PRIMARY);
            add(label);
        }
    }

    /** Small pill badge with count. */
    public static class Badge extends JLabel {
        private final Color badgeColor;

        public Badge(int count, Color badgeColor) {
            super(count > 99 ? "99+" : String.valueOf(count), SwingConstants.CENTER);
            this.badgeColor = badgeColor;
            setFont(font(Font.BOLD, 10));
            setForeground(TEXT);
            setPreferredSize(new Dimension(count > 9 ? 22 : 18, 16));
            setOpaque(false);
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(badgeColor);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), getHeight(), getHeight()));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public record Palette(Color background, Color surface, Color raised, Color input, Color text, Color muted, Color border) { }

    public static class AmbientPanel extends JPanel {
        private final boolean dark;

        public AmbientPanel(boolean dark) {
            this.dark = dark;
            setOpaque(true);
            setDoubleBuffered(true);
        }

        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int width = getWidth();
            int height = getHeight();
            Paint base = dark
                    ? new GradientPaint(0, 0, new Color(12, 16, 21), width, height, new Color(30, 35, 42))
                    : new GradientPaint(0, 0, new Color(248, 242, 236), width, height, new Color(232, 239, 236));
            g.setPaint(base);
            g.fillRect(0, 0, width, height);
            g.setComposite(AlphaComposite.SrcOver.derive(dark ? .22f : .16f));
            g.setColor(new Color(202, 165, 121));
            g.fill(new Ellipse2D.Double(-width * .16, -height * .18, width * .56, height * .62));
            g.setColor(new Color(128, 157, 149));
            g.fill(new Ellipse2D.Double(width * .52, height * .38, width * .58, height * .66));
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    public static class GlassPanel extends JPanel {
        private final boolean dark;
        private final int radius;

        public GlassPanel(boolean dark, int radius) {
            this.dark = dark;
            this.radius = radius;
            setOpaque(false);
            setDoubleBuffered(true);
        }

        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Palette colors = palette(dark);
            g.setColor(colors.surface());
            g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            g.setColor(new Color(colors.border().getRed(), colors.border().getGreen(), colors.border().getBlue(), 90));
            g.setStroke(new BasicStroke(1.1f));
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    public static class GlassButton extends JButton {
        private final Color fill;
        private final Color hover;

        public GlassButton(String text, Color fill, Color foreground) {
            super(text);
            this.fill = fill;
            this.hover = brighten(fill);
            styleButton(this, fill, foreground);
            setDoubleBuffered(true);
        }

        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color color = !isEnabled() ? new Color(fill.getRed(), fill.getGreen(), fill.getBlue(), 90)
                    : getModel().isPressed() ? fill.darker() : getModel().isRollover() ? hover : fill;
            g.setColor(color);
            g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            g.setColor(new Color(255, 255, 255, isEnabled() ? 26 : 12));
            g.fillRoundRect(0, 0, getWidth() - 1, getHeight() / 2, 12, 12);
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    static Color brighten(Color color) {
        return new Color(Math.min(255, color.getRed() + 14), Math.min(255, color.getGreen() + 14),
                Math.min(255, color.getBlue() + 14), color.getAlpha());
    }

    public static final class RoundedBorder extends AbstractBorder {
        private final Color color;
        private final int radius;

        public RoundedBorder(Color color, int radius) { this.color = color; this.radius = radius; }

        @Override public void paintBorder(Component component, Graphics graphics, int x, int y, int width, int height) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(color);
            g.setStroke(new BasicStroke(1f));
            g.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
            g.dispose();
        }

        @Override public Insets getBorderInsets(Component component) { return new Insets(1, 1, 1, 1); }
    }
}
