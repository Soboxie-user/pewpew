package gg.deepsite.pewpew.api.objects.attachment;

import gg.deepsite.pewpew.api.enums.AttachmentType;
import gg.deepsite.pewpew.api.objects.PewPewItem;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public abstract class PewpewAttachment extends PewPewItem {

	private AttachmentType slot;

	/** Stat changes from the 'modifiers' config block; null when the attachment has none. */
	private AttachmentModifiers modifiers;

	/** Hit effects from the 'effects' config block; null when the attachment has none. */
	private AttachmentEffects effects;
}

