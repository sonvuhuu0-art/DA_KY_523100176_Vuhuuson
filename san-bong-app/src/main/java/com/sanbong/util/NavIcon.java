package com.sanbong.util;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

/**
 * Small hand-drawn line-icon set (no emoji, no bundled icon font) used across
 * the sidebar and the login/register feature panel, so every icon renders
 * identically regardless of OS emoji font support.
 */
public final class NavIcon {

    private NavIcon() {
    }

    public static Node build(String name) {
        Group group = switch (name) {
            case "list" -> list();
            case "calendar" -> calendar();
            case "user" -> user();
            case "chart" -> chart();
            case "building" -> building();
            case "field" -> field();
            case "booking" -> booking();
            case "users" -> users();
            case "key" -> key();
            case "logout" -> logout();
            default -> new Group();
        };
        group.getStyleClass().add("nav-icon");
        return group;
    }

    private static Group list() {
        return new Group(
                line(7, 5, 18, 5), line(7, 10, 18, 10), line(7, 15, 18, 15),
                dot(2, 5), dot(2, 10), dot(2, 15));
    }

    private static Group calendar() {
        return new Group(
                rect(2, 4, 16, 14, 3),
                line(6, 1.5, 6, 6), line(14, 1.5, 14, 6),
                line(2, 9, 18, 9));
    }

    private static Group user() {
        return new Group(
                circle(10, 6, 3.2),
                polygon(3, 19, 5, 12.5, 15, 12.5, 17, 19));
    }

    private static Group chart() {
        return new Group(
                rect(3, 11, 3.5, 7, 1), rect(9, 6, 3.5, 12, 1), rect(15, 2, 3.5, 16, 1));
    }

    private static Group building() {
        return new Group(
                rect(4, 2, 12, 16, 1),
                rect(7, 5, 2, 2, 0), rect(11, 5, 2, 2, 0),
                rect(7, 9, 2, 2, 0), rect(11, 9, 2, 2, 0),
                rect(8.5, 12, 3, 6, 0));
    }

    private static Group field() {
        return new Group(
                rect(2, 4, 16, 12, 1),
                line(10, 4, 10, 16),
                circle(10, 10, 2.4));
    }

    private static Group booking() {
        return new Group(
                rect(4, 4, 12, 15, 2),
                rect(7.5, 1.5, 5, 3, 1),
                line(7, 9, 13, 9), line(7, 12, 13, 12), line(7, 15, 11, 15));
    }

    private static Group users() {
        return new Group(
                circle(15, 7, 2.2), polygon(11.5, 18, 12.7, 14, 18.3, 14, 19, 18),
                circle(7, 6, 2.8), polygon(2, 18, 3.5, 13, 10.5, 13, 12, 18));
    }

    private static Group key() {
        return new Group(
                circle(6, 14, 3),
                line(8.5, 11.5, 17, 3),
                line(14, 6.5, 16.2, 8.7),
                line(15.6, 4.9, 17.8, 7.1));
    }

    private static Group logout() {
        return new Group(
                polylineOpen(11, 2, 3, 2, 3, 18, 11, 18),
                line(7, 10, 18, 10),
                polylineOpen(14, 6, 18, 10, 14, 14));
    }

    private static Line line(double x1, double y1, double x2, double y2) {
        Line l = new Line(x1, y1, x2, y2);
        style(l);
        return l;
    }

    private static Circle dot(double cx, double cy) {
        Circle c = new Circle(cx, cy, 1.2);
        c.setFill(Color.TRANSPARENT);
        c.getStyleClass().add("nav-icon-shape");
        c.setStrokeWidth(1.6);
        return c;
    }

    private static Circle circle(double cx, double cy, double r) {
        Circle c = new Circle(cx, cy, r);
        style(c);
        return c;
    }

    private static Rectangle rect(double x, double y, double w, double h, double arc) {
        Rectangle r = new Rectangle(x, y, w, h);
        r.setArcWidth(arc);
        r.setArcHeight(arc);
        style(r);
        return r;
    }

    private static Polygon polygon(double... points) {
        Polygon p = new Polygon(points);
        style(p);
        return p;
    }

    private static Polyline polylineOpen(double... points) {
        Polyline p = new Polyline(points);
        style(p);
        return p;
    }

    private static void style(Shape shape) {
        shape.getStyleClass().add("nav-icon-shape");
        shape.setFill(Color.TRANSPARENT);
        shape.setStrokeWidth(1.6);
        shape.setStrokeLineCap(StrokeLineCap.ROUND);
        shape.setStrokeLineJoin(StrokeLineJoin.ROUND);
    }
}
