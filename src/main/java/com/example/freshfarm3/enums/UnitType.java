package com.example.freshfarm3.enums;

/**
 * How a product is sold and stocked.
 *
 * KG    — vegetables, fruits, herbs, grains. Price is per kg, stock is kept
 *         in kg, buyers order in fixed 0.25 kg (250g) steps.
 * PIECE — items sold individually (e.g. bananas, coconuts). Price is per
 *         piece, buyers order whole pieces, but stock is still kept in kg
 *         — Product.avgPieceWeightGrams converts piece-count to kg so
 *         stock deduction stays accurate even though pricing is per piece.
 * LITER — milk and other liquids. Price is per liter, stock is kept in
 *         liters, buyers order in fixed 0.25 L (250ml) steps.
 */
public enum UnitType {
    KG,
    PIECE,
    LITER
}