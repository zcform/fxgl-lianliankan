package com.zc.llk;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.EntityFactory;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.Spawns;
import com.almasb.fxgl.texture.Texture;
import javafx.scene.Group;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

import static com.almasb.fxgl.dsl.FXGL.*;
import static com.zc.llk.constant.*;

/**
 * Spawns Factory
 * 2024-01-08
 * zhangxl
 */
public class ZcFactory implements EntityFactory {
    @Spawns("grid")
    public Entity grid(SpawnData data) {
        Group group = new Group();

        for (int i = 0; i < w_num + 1; i++) {
            group.getChildren().add(new Line(i * cellWidth, 0, i * cellWidth, h_num * cellWidth));
        }

        for (int i = 0; i < h_num + 1; i++) {
            group.getChildren().add(new Line(0, i * cellWidth, w_num * cellWidth, i * cellWidth));
        }

        group.getChildren().forEach((node) -> {
            Line line = (Line) node;
            line.setStrokeWidth(1);
            line.setStroke(Color.BLACK);
            line.setOpacity(1);
        });

        return entityBuilder()
                .at(left + cellWidth, top + cellWidth)
                .view(group)
                .zIndex(0)
                .neverUpdated()
                .build();
    }

    @Spawns("cell")
    public Entity cell(SpawnData data) {
        LlkCell cell = data.get("cell");

        CellType cellType = cell.getCellType();

        Color color = cellType.getColor();

        Image image = image("llk/" + cellType.getImg(), cellWidth, cellWidth);

        Texture texture = new Texture(image).toColor(color);

        Rectangle rectangle = new Rectangle(cellWidth, cellWidth);
        rectangle.setStrokeWidth(0);
        rectangle.setFill(Color.rgb(255, 255, 255, 0));

        Entity build = entityBuilder()
                .at(cell.getX() * cellWidth + left, cell.getY() * cellWidth + top)
                .view(rectangle)
                .viewWithBBox(texture)
                .with("cell", cell)
                .with("isClick", false)
                .onClick(e -> FXGL.<ZcApplication>getAppCast().cellClick(e))
                .zIndex(10)
                .neverUpdated()
                .build();

        return build;
    }

    @Spawns("select")
    public Entity select(SpawnData data) {
        Rectangle rectangle = new Rectangle(cellWidth, cellWidth);
        rectangle.setStroke(Color.RED);
        rectangle.setStrokeWidth(1);
        rectangle.setFill(Color.rgb(255, 255, 255, 0));

        return entityBuilder(data)
                .view(rectangle)
                .zIndex(5)
                .neverUpdated()
                .build();
    }
}
