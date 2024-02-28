package com.zc.llk;

import com.almasb.fxgl.core.collection.grid.Cell;

/**
 * TODO
 * 2024-02-26
 * zhangxl
 */
public class LlkCell extends Cell {
    private CellType cellType;

    public LlkCell(int x, int y, CellType cellType) {
        super(x, y);
        this.cellType = cellType;
    }

    public CellType getCellType() {
        return cellType;
    }
}
