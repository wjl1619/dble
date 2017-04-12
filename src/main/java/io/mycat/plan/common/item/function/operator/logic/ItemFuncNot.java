package io.mycat.plan.common.item.function.operator.logic;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.expr.SQLNotExpr;

import io.mycat.plan.common.field.Field;
import io.mycat.plan.common.item.Item;
import io.mycat.plan.common.item.function.primary.ItemBoolFunc;


/**
 * This Item represents a <code>X IS TRUE</code> boolean predicate.
 * 
 * @author ActionTech
 * 
 */
public class ItemFuncNot extends ItemBoolFunc {

	public ItemFuncNot(Item a) {
		super(new ArrayList<Item>());
		args.add(a);
	}
	
	@Override
	public final String funcName() {
		return "not";
	}

	@Override
	public Functype functype() {
		return Functype.NOT_FUNC;
	}

	@Override
	public BigInteger valInt() {
		boolean value = args.get(0).valBool();
		this.nullValue = args.get(0).isNull();
		return ((!this.nullValue && value == false) ? BigInteger.ONE : BigInteger.ZERO);
	}
	
	@Override
	public SQLExpr toExpression() {
		return new SQLNotExpr(args.get(0).toExpression());
	}

	@Override
	protected Item cloneStruct(boolean forCalculate, List<Item> calArgs, boolean isPushDown, List<Field> fields) {
		List<Item> newArgs = null;
		if (!forCalculate)
			newArgs = cloneStructList(args);
		else
			newArgs = calArgs;
		return new ItemFuncNot(newArgs.get(0));
	}

}