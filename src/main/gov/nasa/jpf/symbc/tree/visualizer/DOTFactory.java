/*
 * Copyright (C) 2015, United States Government, as represented by the
 * Administrator of the National Aeronautics and Space Administration.
 * All rights reserved.
 *
 * Symbolic Pathfinder (jpf-symbc) is licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gov.nasa.jpf.symbc.tree.visualizer;

import java.awt.Color;
import java.util.LinkedList;
import java.util.List;

import att.grappa.Attribute;
import att.grappa.Edge;
import att.grappa.Graph;
import att.grappa.Node;
import gov.nasa.jpf.jvm.bytecode.IfInstruction;
import gov.nasa.jpf.jvm.bytecode.JVMInvokeInstruction;
import gov.nasa.jpf.symbc.numeric.PathCondition;
import gov.nasa.jpf.symbc.tree.NodeFactory;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.LocalVarInfo;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.VM;
import gov.nasa.jpf.vm.bytecode.InvokeInstruction;
import gov.nasa.jpf.vm.bytecode.ReturnInstruction;

/**
 * @author Kasper Luckow
 */
public class DOTFactory extends NodeFactory<Node> {

  private Graph graph;
  private long uniqueID = 0;
  public DOTFactory(Graph graph) {
    this.graph = graph;
  }

  @Override
  public Node constructNode(Node parentNode, Instruction instr, VM vm) {
    Node targetNode = new Node(graph, ""+this.uniqueID++);
    LinkedList<Attribute> attrs = new LinkedList<Attribute>();
    PathCondition pc = PathCondition.getPC(vm);

    if(instr instanceof InvokeInstruction)
      attrs.addAll(this.getNodeAttr((InvokeInstruction)instr, pc));
    else if(instr instanceof ReturnInstruction)
      attrs.addAll(this.getNodeAttr((ReturnInstruction)instr, pc, vm));
    else if(instr instanceof IfInstruction)
      attrs.addAll(this.getNodeAttr((IfInstruction)instr, pc,vm));
    else
      attrs.addAll(this.getNodeAttr(instr, pc, vm));

    for(Attribute attr : attrs)
      targetNode.setAttribute(attr);

    if(parentNode != null)
      new Edge(graph, parentNode, targetNode);
    return targetNode;
  }

  protected List<Attribute> getNodeAttr(InvokeInstruction instr, PathCondition pc) {
    List<Attribute> attrs = new LinkedList<Attribute>();

    StringBuilder lblBuilder = new StringBuilder();
    lblBuilder.append(instr.getMnemonic()).append("\\n");

    JVMInvokeInstruction invokeInstr = (JVMInvokeInstruction) instr;
    lblBuilder.append("Calling:\\n")
    .append(invokeInstr.getInvokedMethod().getFullName());
    attrs.add(new Attribute(Attribute.NODE, Attribute.LABEL_ATTR, lblBuilder.toString()));
    attrs.add(new Attribute(Attribute.NODE, Attribute.COLOR_ATTR, Color.red));
    attrs.add(new Attribute(Attribute.NODE, Attribute.SHAPE_ATTR, Attribute.BOX_SHAPE));
    return attrs;
  }

  protected List<Attribute> getNodeAttr(ReturnInstruction instr, PathCondition pc, VM vm) {
    List<Attribute> attrs = new LinkedList<Attribute>();

    StringBuilder lblBuilder = new StringBuilder();
    lblBuilder.append(instr.getMnemonic()).append("\\n");
    lblBuilder.append("(").append(instr.getFilePos()).append(")\\n");
    lblBuilder.append(getPathConditionString(pc,vm));
    Instruction next = instr.getNext();
    if(next != null)
      lblBuilder.append("Returning to:\\n").append(next.getMethodInfo().getFullName());
    attrs.add(new Attribute(Attribute.NODE, Attribute.LABEL_ATTR, lblBuilder.toString()));
    attrs.add(new Attribute(Attribute.NODE, Attribute.COLOR_ATTR, Color.red));
    attrs.add(new Attribute(Attribute.NODE, Attribute.SHAPE_ATTR, Attribute.BOX_SHAPE));
    return attrs;
  }

  protected List<Attribute> getNodeAttr(IfInstruction instr, PathCondition pc, VM vm) {
    List<Attribute> attrs = new LinkedList<Attribute>();
    StringBuilder lblBuilder = new StringBuilder();
    lblBuilder.append(instr.getMnemonic()).append("\\n");
    lblBuilder.append("(").append(instr.getFilePos()).append(")\\n");
    lblBuilder.append(getPathConditionString(pc,vm));

    attrs.add(new Attribute(Attribute.NODE, Attribute.LABEL_ATTR, lblBuilder.toString()));
    attrs.add(new Attribute(Attribute.NODE, Attribute.COLOR_ATTR, Color.blue));
    attrs.add(new Attribute(Attribute.NODE, Attribute.SHAPE_ATTR, Attribute.DIAMOND_SHAPE));
    return attrs;
  }

  protected List<Attribute> getNodeAttr(Instruction instr, PathCondition pc, VM vm) {

    List<Attribute> attrs = new LinkedList<Attribute>();
    StringBuilder lblBuilder = new StringBuilder();
    lblBuilder.append(instr.getMnemonic()).append("\\n");
    lblBuilder.append("(").append(instr.getFilePos()).append(")\\n");
    lblBuilder.append(getPathConditionString(pc,vm));
    attrs.add(new Attribute(Attribute.NODE, Attribute.LABEL_ATTR, lblBuilder.toString()));
    attrs.add(new Attribute(Attribute.NODE, Attribute.COLOR_ATTR, Color.black));
    return attrs;
  }

  protected String getPathConditionString(PathCondition pc, VM vm) {
    final StackFrame topFrame = vm.getCurrentThread().getModifiableTopFrame();
    final StringBuilder pcBuilder = new StringBuilder();
    LocalVarInfo[] localVars = topFrame.getLocalVars();
    if (localVars != null) {
      int before = pcBuilder.length();
      for (int i = 0; i < localVars.length; i++) {
        LocalVarInfo localVar = localVars[i];
        final Object attr = topFrame.getLocalAttr(localVar.getSlotIndex());
        if(attr != null) {
          if (i != 0) {
            pcBuilder.append("&&\\n");
          }
          pcBuilder.append(localVar.getName());
          pcBuilder.append("=");
          pcBuilder.append(attr.toString());
        }
      }
      if(before != pcBuilder.length()){
        pcBuilder.append("&&\\n");
      }
    }

    if (pc != null) {
      String[] pcs = pc.header.stringPC().split("&&");
      for (int i = 0; i < pcs.length; i++) {
        pcBuilder.append(pcs[i]);
        if (i != pcs.length - 1)
          pcBuilder.append(" &&\\n");
      }
    }
    return pcBuilder.append("\r").toString();
  }
}
