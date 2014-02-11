/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.beam.framework.ui.product;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.swing.binding.Binding;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.ComponentAdapter;
import com.bc.ceres.swing.binding.PropertyEditor;
import com.bc.ceres.swing.binding.internal.TextComponentAdapter;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.ExpressionConverter;
import org.esa.beam.framework.ui.ModalDialog;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;


/**
 * A value editor for band arithmetic expressions
 *
 * @author Marco Zuehlke
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
public class ExpressionEditor extends PropertyEditor {
    
    private Product currentProduct;

    @Override
    public JComponent createEditorComponent(PropertyDescriptor propertyDescriptor, BindingContext bindingContext) {
        JTextField textField = new JTextField();
        ComponentAdapter adapter = new TextComponentAdapter(textField);
        final Binding binding = bindingContext.bind(propertyDescriptor.getName(), adapter);
        final JPanel subPanel = new JPanel(new BorderLayout(2, 2));
        subPanel.add(textField, BorderLayout.CENTER);
        final JButton etcButton = new JButton("...");
        etcButton.setEnabled(false);
        etcButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ProductExpressionPane expressionPane = ProductExpressionPane.createGeneralExpressionPane(
                        new Product[]{currentProduct}, currentProduct, null);
                expressionPane.setCode((String) binding.getPropertyValue());
                if (expressionPane.showModalDialog(null, "Expression Editor") == ModalDialog.ID_OK) {
                    binding.setPropertyValue(expressionPane.getCode());
                }
            }
        });
        bindingContext.getPropertySet().addProperty(Property.create("SOURCE_PRODUCT", Product.class, null, false));
        bindingContext.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals("SOURCE_PRODUCT")) {
                    if (evt.getNewValue() != null) {
                        currentProduct = (Product) evt.getNewValue();
                    }
                    etcButton.setEnabled(currentProduct != null);
                }
            }
        });
        subPanel.add(etcButton, BorderLayout.EAST);
        return subPanel;
    }

    @Override
    public boolean isValidFor(PropertyDescriptor propertyDescriptor) {
        return propertyDescriptor.getConverter() instanceof ExpressionConverter;
    }
}
