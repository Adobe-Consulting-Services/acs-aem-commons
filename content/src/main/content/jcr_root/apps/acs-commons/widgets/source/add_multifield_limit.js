/*global CQ: false */
(function() {
    var originalAddItemFunction = CQ.form.MultiField.prototype.addItem,
        originalValidateFunction = CQ.form.MultiField.prototype.validate;
    CQ.Ext.override(CQ.form.MultiField, {
        getActualItemCount: function() {
            return this.items.getCount() - 1;
        },
        addItem: function(value) {
            if (this.maxItems && (this.maxItems === this.getActualItemCount())) {
                CQ.Ext.Msg.show({
                    title: 'Maximum Items reached',
                    msg: 'You are only allowed to add ' + this.maxItems + ' items to this field',
                    icon: CQ.Ext.MessageBox.WARNING,
                    buttons: CQ.Ext.Msg.OK
                });
                return;
            }
            originalAddItemFunction.apply(this, [value]);
        },
        validate: function() {
            if (this.minItems) {
                if (this.getActualItemCount() < this.minItems) {
                    this.markInvalid("You must add at least " + this.minItems + " items to this field");
                    return false;
                }
            }
            
            return originalValidateFunction.apply(this);
        },
        markInvalid : function(msg){
            //don't set the error icon if we're not rendered or marking is prevented
            if (this.rendered && !this.preventMark) {
                this.body.addClass(this.invalidClass);
            }
            
            this.fireEvent('invalid', this, msg);
        },
        clearInvalid : function(){
            //don't remove the error icon if we're not rendered or marking is prevented
            if (this.rendered && !this.isDestroyed && !this.preventMark) {
                this.body.removeClass(this.invalidClass);
            }
            
            this.fireEvent('valid', this);
        }
    });
}());