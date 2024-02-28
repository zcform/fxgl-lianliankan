package com.zc.llk;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import com.zc.llk.data.LvRecord;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.almasb.fxgl.dsl.FXGL.*;
import static com.zc.llk.constant.*;

public class ZcApplication extends GameApplication {
    LlkCell[][] cells = new LlkCell[w_num + 2][h_num + 2];

    Entity[] selectedEntitys = new Entity[3];
    Entity[] selectedReds = new Entity[2];

    Group lineGroup = new Group();

    Text timeText = new Text();

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setTitle(" ");
        settings.setVersion(" ");
        settings.setAppIcon("zc.png");
        settings.setWidth(left + right + (w_num + 2) * cellWidth);
        settings.setHeight(top + down + (h_num + 2) * cellWidth);
    }

    @Override
    protected void initGameVars(Map<String, Object> vars) {
        vars.put("isStart", false);
        vars.put("lv", 0);
    }

    @Override
    protected void initGame() {
        getGameWorld().addEntityFactory(new ZcFactory());
        getGameScene().setBackgroundColor(new Color(0.1, 0.1, 0.1, 0.1));

        spawn("grid");
        initLv();
    }

    @Override
    protected void initUI() {
        drawTop();
    }

    @Override
    protected void onUpdate(double tpf) {
        if (getb("isStart")) {
            long currentTime = System.currentTimeMillis();
            long beginTime = geto("beginTime");

            timeText.setText((currentTime - beginTime) / 1000 + " s");
        }
    }

    private void drawTop() {
        Line line = new Line(0, topTitle, getAppWidth(), topTitle);
        line.setStroke(Color.GAINSBORO);
        line.setStrokeWidth(2);

        timeText.setText("0 s");
        timeText.setX(getAppWidth() - 50);
        timeText.setY(topTitle / 2 + 8);
        timeText.setFont(new Font(16));

        addUINode(line);
        addUINode(timeText);
    }

    private void initLv() {
        int lv = geti("lv");

        LvRecord lvRecord = getAssetLoader().loadJSON("levels/lv" + lv + ".json", LvRecord.class).get();

        List<CellType> cellTypes = new ArrayList<>();

        for (String img : lvRecord.img()) {
            for (String color : lvRecord.color()) {
                cellTypes.add(new CellType(img, Color.valueOf(color)));
            }
        }

        int cellNum = w_num * h_num;

        for (int i = 0; i < cellNum / 2; i++) {
            int random = random(0, cellTypes.size() - 1);
            CellType cellType = cellTypes.get(random);

            createCell(cellType);
            createCell(cellType);
        }
    }

    private void createCell(CellType cellType) {
        List<Point2D> cellPoints = new ArrayList<>();

        for (int i = 1; i <= w_num; i++) {
            for (int j = 1; j <= h_num; j++) {
                if (cells[i][j] == null) {
                    cellPoints.add(new Point2D(i, j));
                }
            }
        }

        Point2D point = cellPoints.get(random(0, cellPoints.size() - 1));

        int x = (int) point.getX();
        int y = (int) point.getY();

        cells[x][y] = new LlkCell(x, y, cellType);
        spawn("cell", new SpawnData().put("cell", cells[x][y]));
    }

    /**
     * Entity选中
     */
    public void cellClick(Entity entity) {
        if (!getb("isStart")) {
            set("isStart", true);
            set("beginTime", System.currentTimeMillis());
        }

        if (entity.getBoolean("isClick")) {
            clearSel();
            return;
        }

        entity.setProperty("isClick", true);

        Entity spawn = spawn("select", entity.getX(), entity.getY());

        if (selectedEntitys[0] == null) {
            selectedEntitys[0] = entity;
            selectedReds[0] = spawn;
        } else {
            selectedEntitys[1] = entity;
            selectedReds[1] = spawn;
        }

        selectedEntitys[2] = entity;

        if (selectedEntitys[1] != null) {
            check();
        }
    }

    /**
     * 判断选中两点是否可连接，
     * 可连接则清除并判断是否完成本关卡
     * 不能连接清除选中标记
     */
    private void check() {
        //校验
        boolean canLink = canLink();

        Entity entity1 = selectedEntitys[0];
        Entity entity2 = selectedEntitys[1];

        LlkCell cell1 = entity1.getObject("cell");
        LlkCell cell2 = entity2.getObject("cell");

        if (canLink) {
            addUINode(lineGroup);

            runOnce(() -> {
                cells[cell1.getX()][cell1.getY()] = null;
                cells[cell2.getX()][cell2.getY()] = null;

                entity1.removeFromWorld();
                entity2.removeFromWorld();

                clearSel();
                isOver();
            }, Duration.seconds(.1));
        } else {
            clearSel();

            Entity entity = selectedEntitys[2];
            entity.setProperty("isClick", true);
            Entity spawn = spawn("select", entity.getX(), entity.getY());

            selectedEntitys[0] = entity;
            selectedReds[0] = spawn;
        }
    }

    private void isOver() {
        for (LlkCell[] cellx : cells) {
            for (LlkCell cell : cellx) {
                if (cell != null) {
                    return;
                }
            }
        }

        // todo 下一关
        // over -> next lever
        //FXGL.inc("lv", 1);

        initLv();
    }

    /**
     * 是否可以连接
     *
     * @return
     */
    private boolean canLink() {
        if (selectedEntitys[0] == null || selectedEntitys[1] == null) {
            return false;
        }

        LlkCell cell1 = selectedEntitys[0].getObject("cell");
        LlkCell cell2 = selectedEntitys[1].getObject("cell");

        if (cell1.getCellType() != cell2.getCellType()) {
            return false;
        }

        // 是否只用1条线就可连接
        if (lineOne()) {
            return true;
        }

        // 是否只用2条线就可连接
        if (lineTwo()) {
            return true;
        }

        // 是否只用3条线就可连接
        return lineThree();
    }

    /**
     * 是否只用3条线就可连接
     *
     * @return
     */
    private boolean lineThree() {
        LlkCell cell1 = selectedEntitys[0].getObject("cell");
        LlkCell cell2 = selectedEntitys[1].getObject("cell");

        Point2D pointS = new Point2D(cell1.getX(), cell1.getY());
        Point2D pointE = new Point2D(cell2.getX(), cell2.getY());

        // 从上到下
        for (int i = 0; i < cells[0].length; i++) {
            Point2D point1 = new Point2D(cell1.getX(), i);
            Point2D point2 = new Point2D(cell2.getX(), i);

            if (pointS.distance(point1) == 0 || point2.distance(pointE) == 0) {
                continue;
            }

            if (pointToCell(point1) != null || pointToCell(point2) != null) {
                continue;
            }

            if (isEnemyLine(pointS, point1) && isEnemyLine(point2, pointE) && isEnemyLine(point1, point2)) {
                createLint(pointS, point1);
                createLint(point2, pointE);
                createLint(point1, point2);

                return true;
            }
        }

        // 从左往右
        for (int i = 0; i < cells.length; i++) {
            Point2D point1 = new Point2D(i, cell1.getY());
            Point2D point2 = new Point2D(i, cell2.getY());

            if (pointS.distance(point1) == 0 || point2.distance(pointE) == 0) {
                continue;
            }

            if (pointToCell(point1) != null || pointToCell(point2) != null) {
                continue;
            }

            if (isEnemyLine(pointS, point1) && isEnemyLine(point2, pointE) && isEnemyLine(point1, point2)) {
                createLint(pointS, point1);
                createLint(point2, pointE);
                createLint(point1, point2);

                return true;
            }
        }

        return false;
    }

    /**
     * 是否只用2条线就可连接
     *
     * @return
     */
    private boolean lineTwo() {
        LlkCell cell1 = selectedEntitys[0].getObject("cell");
        LlkCell cell2 = selectedEntitys[1].getObject("cell");

        if (cell1.getX() == cell2.getX() || cell1.getY() == cell2.getY()) {
            return false;
        }

        Point2D point1 = new Point2D(cell1.getX(), cell1.getY());
        Point2D point2 = null;
        Point2D point3 = new Point2D(cell2.getX(), cell2.getY());

        boolean b1 = false;
        boolean b2 = false;

        LlkCell cellz1 = cells[cell2.getX()][cell1.getY()];

        if (cellz1 == null) {
            point2 = new Point2D(cell2.getX(), cell1.getY());

            if (cells[cell2.getX()][cell1.getY()] == null) {
                b1 = isEnemyLine(point1, point2);
                b2 = isEnemyLine(point2, point3);

                if (b1 && b2) {
                    createLint(point1, point2);
                    createLint(point2, point3);

                    return true;
                }
            }
        }

        LlkCell cellz2 = cells[cell1.getX()][cell2.getY()];

        if (cellz2 == null) {
            point2 = new Point2D(cell1.getX(), cell2.getY());

            if (cells[cell1.getX()][cell2.getY()] == null) {
                b1 = isEnemyLine(point1, point2);
                b2 = isEnemyLine(point2, point3);

                if (b1 && b2) {
                    createLint(point1, point2);
                    createLint(point2, point3);

                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 是否只用1条线就可连接
     *
     * @return
     */
    private boolean lineOne() {
        LlkCell cell1 = selectedEntitys[0].getObject("cell");
        LlkCell cell2 = selectedEntitys[1].getObject("cell");

        Point2D point1 = cellToPoint(cell1);
        Point2D point2 = cellToPoint(cell2);

        boolean flag = false;

        // 若相邻
        if (point1.distance(point2) == 1) {
            if (cell1.getCellType() == cell2.getCellType()) {
                flag = true;
            }
        } else {
            flag = isEnemyLine(point1, point2);
        }

        if (flag) {
            createLint(point1, point2);
        }

        return flag;
    }

    /**
     * 两点是否可以连接
     *
     * @param point1
     * @param point2
     * @return
     */
    private boolean isEnemyLine(Point2D point1, Point2D point2) {
        // 必须在同行/列
        if (point1.getX() != point2.getX() && point1.getY() != point2.getY()) {
            return false;
        }

        // 若相邻，其中一个为空即可连接
        if (point1.distance(point2) == 1) {
            if (pointToCell(point1) == null || pointToCell(point2) == null) {
                return true;
            }
        }

        boolean flag = false;

        if (point1.getX() == point2.getX()) {
            int min = (int) Math.min(point1.getY(), point2.getY());
            int max = (int) Math.max(point1.getY(), point2.getY());

            if (max - min == 1) {
                return true;
            }

            for (int i = min + 1; i < max; i++) {
                LlkCell cell = cells[(int) point1.getX()][i];

                if (cell != null) {
                    flag = true;
                    break;
                }
            }
        }

        if (point1.getY() == point2.getY()) {
            int min = (int) Math.min(point1.getX(), point2.getX());
            int max = (int) Math.max(point1.getX(), point2.getX());

            if (max - min == 1) {
                return true;
            }

            for (int i = min + 1; i < max; i++) {
                LlkCell cell = cells[i][(int) point1.getY()];

                if (cell != null) {
                    flag = true;
                    break;
                }
            }
        }

        if (!flag) {
            return true;
        }

        return false;
    }

    /**
     * 创建连接线
     *
     * @param point1
     * @param point2
     */
    private void createLint(Point2D point1, Point2D point2) {
        var p1x = point1.getX() * cellWidth + left + cellWidth / 2;
        var p1y = point1.getY() * cellWidth + top + cellWidth / 2;

        var p2x = point2.getX() * cellWidth + left + cellWidth / 2;
        var p2y = point2.getY() * cellWidth + top + cellWidth / 2;

        Line line = new Line(p1x, p1y, p2x, p2y);
        line.setStrokeWidth(1);
        line.setStroke(Color.BLUE);

        lineGroup.getChildren().add(line);
    }

    /**
     * 清除选中状态和连线
     */
    private void clearSel() {
        if (selectedEntitys[0] != null) {
            selectedEntitys[0].setProperty("isClick", false);
            selectedEntitys[0] = null;
        }
        if (selectedEntitys[1] != null) {
            selectedEntitys[1].setProperty("isClick", false);
            selectedEntitys[1] = null;
        }
        if (selectedReds[0] != null) {
            selectedReds[0].removeFromWorld();
            selectedReds[0] = null;
        }
        if (selectedReds[1] != null) {
            selectedReds[1].removeFromWorld();
            selectedReds[1] = null;
        }

        removeUINode(lineGroup);
        lineGroup = new Group();
    }

    private LlkCell pointToCell(Point2D point) {
        return cells[(int) point.getX()][(int) point.getY()];
    }

    private Point2D cellToPoint(LlkCell cell) {
        return new Point2D(cell.getX(), cell.getY());
    }

    public static void main(String[] args) {
        //new Color(.1, .1, .1, .1);
        //
        //File file = new File("E:\\ideaWorkSpace\\zcStudy\\00000\\000000FXGL-git\\" +
        //        "fxgl-lianliankan\\src\\main\\resources\\assets\\textures\\llk");
        //
        //for (File listFile : file.listFiles()) {
        //    System.out.println("\"" + listFile.getName() + "\",");
        //}

        launch(args);
    }
}