/*
 *  Copyright 2016 Alexey Andreev.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.teavm.model.text;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.teavm.model.*;
import org.teavm.model.instructions.*;

class InstructionStringifier implements InstructionReader {
    private TextLocation location;
    private StringBuilder sb;

    InstructionStringifier(StringBuilder sb) {
        this.sb = sb;
    }

    public TextLocation getLocation() {
        return location;
    }

    @Override
    public void location(TextLocation location) {
        this.location = location;
    }

    @Override
    public void nop() {
        sb.append("nop");
    }

    @Override
    public void classConstant(VariableReader receiver, ValueType cst) {
        sb.append("@").append(receiver.getIndex()).append(" := classOf ");
        escapeIdentifierIfNeeded(cst.toString(), sb);
    }

    @Override
    public void nullConstant(VariableReader receiver) {
        sb.append("@").append(receiver.getIndex()).append(" := null");
    }

    @Override
    public void integerConstant(VariableReader receiver, int cst) {
        sb.append("@").append(receiver.getIndex()).append(" := ").append(cst);
    }

    @Override
    public void longConstant(VariableReader receiver, long cst) {
        sb.append("@").append(receiver.getIndex()).append(" := ").append(cst).append("L");
    }

    @Override
    public void floatConstant(VariableReader receiver, float cst) {
        sb.append("@").append(receiver.getIndex()).append(" := ").append(cst).append('F');
    }

    @Override
    public void doubleConstant(VariableReader receiver, double cst) {
        sb.append("@").append(receiver.getIndex()).append(" := ").append(cst);
    }

    @Override
    public void stringConstant(VariableReader receiver, String cst) {
        sb.append("@").append(receiver.getIndex()).append(" := '");
        escapeStringLiteral(cst, sb);
        sb.append("'");
    }

    static void escapeStringLiteral(String s, StringBuilder sb) {
        for (int i = 0; i < s.length(); ++i) {
            char c = s.charAt(i);
            switch (c) {
                case '\n':
                    sb.append("\\n");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\'':
                    sb.append("\\'");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                default:
                    if (c < ' ') {
                        sb.append("\\u");
                        int pos = 12;
                        for (int j = 0; j < 4; ++j) {
                            sb.append(Character.forDigit((c >> pos) & 0xF, 16));
                            pos -= 4;
                        }
                    } else {
                        sb.append(c);
                    }
                    break;
            }
        }
    }

    private static void escapeIdentifierIfNeeded(String s, StringBuilder sb) {
        boolean needsEscaping = false;
        if (s.isEmpty()) {
            needsEscaping = true;
        } else if (!ListingLexer.isIdentifierStart(s.charAt(0))) {
                needsEscaping = true;
        } else {
            for (int i = 1; i < s.length(); ++i) {
                if (!ListingLexer.isIdentifierPart(s.charAt(i))) {
                    needsEscaping = true;
                    break;
                }
            }
        }
        if (needsEscaping) {
            sb.append('`').append(s).append('`');
        } else {
            sb.append(s);
        }
    }

    @Override
    public void binary(BinaryOperation op, VariableReader receiver, VariableReader first, VariableReader second,
            NumericOperandType type) {
        sb.append("@").append(receiver.getIndex()).append(" := @").append(first.getIndex()).append(" ");
        switch (op) {
            case ADD:
                sb.append("+");
                break;
            case AND:
                sb.append("&");
                break;
            case COMPARE:
                sb.append("compareTo");
                break;
            case DIVIDE:
                sb.append("/");
                break;
            case MODULO:
                sb.append("%");
                break;
            case MULTIPLY:
                sb.append("*");
                break;
            case OR:
                sb.append("|");
                break;
            case SHIFT_LEFT:
                sb.append("<<");
                break;
            case SHIFT_RIGHT:
                sb.append(">>");
                break;
            case SHIFT_RIGHT_UNSIGNED:
                sb.append(">>>");
                break;
            case SUBTRACT:
                sb.append("-");
                break;
            case XOR:
                sb.append("^");
                break;
        }
        sb.append(" @").append(second.getIndex());
    }

    @Override
    public void negate(VariableReader receiver, VariableReader operand, NumericOperandType type) {
        sb.append("@").append(receiver.getIndex()).append(" := -").append(" @").append(operand.getIndex());
    }

    @Override
    public void assign(VariableReader receiver, VariableReader assignee) {
        sb.append("@").append(receiver.getIndex()).append(" := @").append(assignee.getIndex());
    }

    @Override
    public void cast(VariableReader receiver, VariableReader value, ValueType targetType) {
        sb.append("@").append(receiver.getIndex()).append(" := cast @").append(value.getIndex())
                .append(" to ");
        escapeIdentifierIfNeeded(targetType.toString(), sb);
    }

    @Override
    public void cast(VariableReader receiver, VariableReader value, NumericOperandType sourceType,
            NumericOperandType targetType) {
        sb.append("@").append(receiver.getIndex()).append(" := cast @").append(value.getIndex())
                .append(" from ").append(sourceType).append(" to ").append(targetType);
    }

    @Override
    public void cast(VariableReader receiver, VariableReader value, IntegerSubtype type,
            CastIntegerDirection direction) {
        sb.append("@").append(receiver.getIndex()).append(" := cast @").append(value.getIndex());
        switch (direction) {
            case FROM_INTEGER:
                sb.append(" from int to ").append(type.name().toLowerCase(Locale.ROOT));
                break;
            case TO_INTEGER:
                sb.append(" from ").append(type.name().toLowerCase(Locale.ROOT)).append(" to int");
                break;
        }
    }

    @Override
    public void jumpIf(BranchingCondition cond, VariableReader operand, BasicBlockReader consequent,
            BasicBlockReader alternative) {
        sb.append("if @").append(operand.getIndex()).append(" ");
        switch (cond) {
            case EQUAL:
                sb.append("== 0");
                break;
            case NOT_EQUAL:
                sb.append("!= 0");
                break;
            case GREATER:
                sb.append("> 0");
                break;
            case GREATER_OR_EQUAL:
                sb.append(">= 0");
                break;
            case LESS:
                sb.append("<= 0");
                break;
            case LESS_OR_EQUAL:
                sb.append("< 0");
                break;
            case NOT_NULL:
                sb.append("!== null");
                break;
            case NULL:
                sb.append("=== null");
                break;
        }
        sb.append(" then goto $").append(consequent.getIndex()).append(" else goto $").append(alternative.getIndex());
    }

    @Override
    public void jumpIf(BinaryBranchingCondition cond, VariableReader first, VariableReader second,
            BasicBlockReader consequent, BasicBlockReader alternative) {
        sb.append("if @").append(first.getIndex()).append(" ");
        switch (cond) {
            case EQUAL:
                sb.append("==");
                break;
            case REFERENCE_EQUAL:
                sb.append("===");
                break;
            case NOT_EQUAL:
                sb.append("!=");
                break;
            case REFERENCE_NOT_EQUAL:
                sb.append("!==");
                break;
        }
        sb.append("@").append(second.getIndex()).append(" then goto $").append(consequent.getIndex())
                .append(" else goto $").append(alternative.getIndex());
    }

    @Override
    public void jump(BasicBlockReader target) {
        sb.append("goto $").append(target.getIndex());
    }

    @Override
    public void choose(VariableReader condition, List<? extends SwitchTableEntryReader> table,
            BasicBlockReader defaultTarget) {
        sb.append("switch @").append(condition.getIndex()).append(" ");
        for (int i = 0; i < table.size(); ++i) {
            if (i > 0) {
                sb.append("; ");
            }
            SwitchTableEntryReader entry = table.get(i);
            sb.append("if ").append(entry.getCondition()).append(" goto $").append(entry.getTarget().getIndex());
        }
        sb.append(" else goto $").append(defaultTarget.getIndex());
    }

    @Override
    public void exit(VariableReader valueToReturn) {
        sb.append("return");
        if (valueToReturn != null) {
            sb.append(" @").append(valueToReturn.getIndex());
        }
    }

    @Override
    public void raise(VariableReader exception) {
        sb.append("throw @").append(exception.getIndex());
    }

    @Override
    public void createArray(VariableReader receiver, ValueType itemType, VariableReader size) {
        sb.append("@").append(receiver.getIndex()).append(" := new ");
        escapeIdentifierIfNeeded(itemType.toString(), sb);
        sb.append("[@").append(size.getIndex()).append(']');
    }

    @Override
    public void createArray(VariableReader receiver, ValueType itemType, List<? extends VariableReader> dimensions) {
        sb.append("@").append(receiver.getIndex()).append(" := newArray ");
        escapeIdentifierIfNeeded(itemType.toString(), sb);
        sb.append("[");

        for (int i = 0; i < dimensions.size(); ++i) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append("@").append(dimensions.get(i).getIndex());
        }
        sb.append("]");
    }

    @Override
    public void create(VariableReader receiver, String type) {
        sb.append("@").append(receiver.getIndex()).append(" := newArray ");
        escapeIdentifierIfNeeded(type, sb);
    }

    @Override
    public void getField(VariableReader receiver, VariableReader instance, FieldReference field, ValueType fieldType) {
        sb.append("@").append(receiver.getIndex()).append(" := field ");
        escapeIdentifierIfNeeded(field.toString(), sb);
        sb.append(field);
        if (instance != null) {
            sb.append(" @").append(instance.getIndex());
        }
    }

    @Override
    public void putField(VariableReader instance, FieldReference field, VariableReader value, ValueType fieldType) {
        sb.append("field ");
        escapeIdentifierIfNeeded(field.toString(), sb);
        if (instance != null) {
            sb.append(" @").append(instance.getIndex());
        }
        sb.append(" := @").append(value.getIndex());
    }

    @Override
    public void arrayLength(VariableReader receiver, VariableReader array) {
        sb.append("@").append(receiver.getIndex()).append(" := lengthOf @").append(array.getIndex());
    }

    @Override
    public void cloneArray(VariableReader receiver, VariableReader array) {
        sb.append("@").append(receiver.getIndex()).append(" := clone @").append(array.getIndex());
    }

    @Override
    public void unwrapArray(VariableReader receiver, VariableReader array, ArrayElementType elementType) {
        sb.append("@").append(receiver.getIndex()).append(" := data @").append(array.getIndex()).append(" as ")
                .append(elementType.name().toLowerCase(Locale.ROOT));
    }

    @Override
    public void getElement(VariableReader receiver, VariableReader array, VariableReader index,
            ArrayElementType type) {
        sb.append("@").append(receiver.getIndex()).append(" := @").append(array.getIndex()).append("[@")
                .append(index.getIndex()).append("]").append(" as " + type.name().toLowerCase(Locale.ROOT));
    }

    @Override
    public void putElement(VariableReader array, VariableReader index, VariableReader value, ArrayElementType type) {
        sb.append("@").append(array.getIndex()).append("[@").append(index.getIndex()).append("] := @")
                .append(value.getIndex()).append(" as " + type.name().toLowerCase());
    }

    @Override
    public void invoke(VariableReader receiver, VariableReader instance, MethodReference method,
            List<? extends VariableReader> arguments, InvocationType type) {
        if (receiver != null) {
            sb.append("@").append(receiver.getIndex()).append(" := ");
        }
        if (instance == null) {
            sb.append("invokeStatic ");
        } else {
            switch (type) {
                case SPECIAL:
                    sb.append("invoke ");
                    break;
                case VIRTUAL:
                    sb.append("invokeVirtual ");
                    break;
            }
        }

        escapeIdentifierIfNeeded(method.toString(), sb);
        if (instance != null) {
            sb.append("@").append(instance.getIndex());
        }

        for (int i = 0; i < arguments.size(); ++i) {
            if (instance != null || i > 0) {
                sb.append(", ");
            }
            sb.append("@").append(arguments.get(i).getIndex());
        }
    }

    @Override
    public void invokeDynamic(VariableReader receiver, VariableReader instance, MethodDescriptor method,
            List<? extends VariableReader> arguments, MethodHandle bootstrapMethod,
            List<RuntimeConstant> bootstrapArguments) {
        if (receiver != null) {
            sb.append("@").append(receiver.getIndex()).append(" := ");
        }
        if (instance != null) {
            sb.append("@").append(instance.getIndex()).append(".");
        }
        sb.append(method.getName()).append("(");
        sb.append(arguments.stream().map(arg -> "@"  + arg.getIndex()).collect(Collectors.joining(", ")));
        sb.append(") ");
        sb.append("[").append(convert(bootstrapMethod)).append('(');
        sb.append(bootstrapArguments.stream().map(this::convert).collect(Collectors.joining(", ")));
        sb.append(")");
    }

    private String convert(MethodHandle handle) {
        switch (handle.getKind()) {
            case INVOKE_VIRTUAL:
            case INVOKE_SPECIAL:
            case INVOKE_INTERFACE:
                return new MethodDescriptor(handle.getName(), handle.signature()).toString();
            case INVOKE_CONSTRUCTOR:
                return "new" + handle.getClassName() + "." + new MethodDescriptor(handle.getName(),
                        handle.signature()).toString();
            case INVOKE_STATIC:
                return handle.getClassName() + "." + new MethodDescriptor(handle.getName(),
                        handle.signature()).toString();
            case GET_FIELD:
                return "GET " + handle.getName();
            case GET_STATIC_FIELD:
                return "GET " + handle.getClassName() + "." + handle.getName();
            case PUT_FIELD:
                return "PUT " + handle.getName();
            case PUT_STATIC_FIELD:
                return "PUT " + handle.getClassName() + "." + handle.getName();
        }
        throw new IllegalArgumentException("Unexpected handle type: " + handle.getKind());
    }

    private String convert(RuntimeConstant cst) {
        switch (cst.getKind()) {
            case RuntimeConstant.INT:
                return String.valueOf(cst.getInt());
            case RuntimeConstant.LONG:
                return String.valueOf(cst.getLong());
            case RuntimeConstant.FLOAT:
                return String.valueOf(cst.getFloat());
            case RuntimeConstant.DOUBLE:
                return String.valueOf(cst.getDouble());
            case RuntimeConstant.STRING:
                return String.valueOf(cst.getString());
            case RuntimeConstant.TYPE:
                return String.valueOf(cst.getValueType());
            case RuntimeConstant.METHOD: {
                ValueType[] methodType = cst.getMethodType();
                return "(" + Arrays.stream(methodType, 0, methodType.length - 1).map(Object::toString)
                            .collect(Collectors.joining()) + ")" + methodType[methodType.length - 1];
            }
            case RuntimeConstant.METHOD_HANDLE:
                return convert(cst.getMethodHandle());
        }
        throw new IllegalArgumentException("Unexpected runtime constant type: " + cst.getKind());
    }

    @Override
    public void isInstance(VariableReader receiver, VariableReader value, ValueType type) {
        sb.append("@").append(receiver.getIndex()).append(" := @").append(value.getIndex())
                .append(" instanceOf ");
        escapeIdentifierIfNeeded(type.toString(), sb);
    }

    @Override
    public void initClass(String className) {
        sb.append("initClass ").append(className);
    }

    @Override
    public void nullCheck(VariableReader receiver, VariableReader value) {
        sb.append("@").append(receiver.getIndex()).append(" := nullCheck @").append(value.getIndex());
    }

    @Override
    public void monitorEnter(VariableReader objectRef) {
        sb.append("monitorEnter @").append(objectRef.getIndex());
    }

    @Override
    public void monitorExit(VariableReader objectRef) {
        sb.append("monitorExit @").append(objectRef.getIndex());
    }
}
