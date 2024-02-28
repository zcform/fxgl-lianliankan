package com.zc.llk;

import cn.hutool.core.util.StrUtil;
import javafx.scene.paint.Color;

/**
 * TODO
 * 2024-02-26
 * zhangxl
 */
public class CellType {
    private String img;
    private Color color;

    public CellType(String img) {
        init(img, Color.BLACK);
    }

    public CellType(String img, Color color) {
        init(img, color);
    }

    private void init(String img, Color color) {
        if (StrUtil.isEmpty(img)) {
            throw new RuntimeException("img is null");
        }

        this.img = img;
        this.color = Color.BLACK;

        if (color != null && color != Color.BLACK) {
            this.color = color;
        }
    }

    public String getImg() {
        return img;
    }

    public Color getColor() {
        return color;
    }

    @Override
    public String toString() {
        return String.format("CellType[img=%s,color=%s]", this.img, this.color);
    }
}
