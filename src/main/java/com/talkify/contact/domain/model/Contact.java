package com.talkify.contact.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.talkify.common.domain.AggregateRoot;
import com.talkify.common.domain.UserId;
import com.talkify.contact.domain.event.ContactRequestAcceptedEvent;
import com.talkify.contact.domain.event.ContactRequestSentEvent;
import com.talkify.contact.domain.exception.ContactNotFoundException;
import com.talkify.contact.domain.exception.ContactPermissionException;
import com.talkify.contact.domain.exception.ContactStatusViolationException;
import com.talkify.contact.domain.exception.SelfContactRequestException;

public class Contact extends AggregateRoot {
    private final UUID id; // UUIDv7 công khai
    private final UserId requesterId;
    private final UserId addresseeId;
    private ContactStatus status;
    
    // private String nicknameByRequester;
    // private String nicknameByAddressee;
    // private boolean isRestrictedByRequester;
    // private boolean isRestrictedByAddressee;

    private Contact(UUID id, UserId requesterId, UserId addresseeId, ContactStatus status) {
        this.id = Objects.requireNonNull(id);
        this.requesterId = Objects.requireNonNull(requesterId);
        this.addresseeId = Objects.requireNonNull(addresseeId);
        this.status = status;
    }

    public static Contact reconstitute(UUID id, UserId requesterId, UserId addresseeId, ContactStatus status
                                       //String nicknameByRequester, String nicknameByAddressee,
                                       //boolean isRestrictedByRequester, boolean isRestrictedByAddressee
                                       ) {
        Contact contact = new Contact(id, requesterId, addresseeId, status);
        // contact.nicknameByRequester = nicknameByRequester;
        // contact.nicknameByAddressee = nicknameByAddressee;
        // contact.isRestrictedByRequester = isRestrictedByRequester;
        // contact.isRestrictedByAddressee = isRestrictedByAddressee;
        return contact;
    }

    public static Contact createRequest(UUID id, UserId requesterId, UserId addresseeId) {
        if (requesterId.equals(addresseeId)) {
            throw new SelfContactRequestException();
        }
        Contact contact = new Contact(id, requesterId, addresseeId, ContactStatus.PENDING);
        contact.registerEvent(new ContactRequestSentEvent(contact.id.toString(), requesterId.value(), addresseeId.value()));
        return contact;
    }

    public void acceptRequest(UserId actorId) {
        if (this.status != ContactStatus.PENDING) {
            throw new ContactStatusViolationException();
        }

        if (!this.addresseeId.equals(actorId)) {
            throw new ContactNotFoundException();
        }
        
        this.status = ContactStatus.ACCEPTED;
        this.registerEvent(new ContactRequestAcceptedEvent(this.id.toString(), this.requesterId.value(), this.addresseeId.value()));
    }

    public void rejectRequest(UserId actorId) {
        if (this.status != ContactStatus.PENDING) {
            throw new ContactStatusViolationException();
        }
        if (!this.addresseeId.equals(actorId)) {
            throw new ContactNotFoundException();
        }
    }

    public void cancelRequest(UserId actorId) {
        if (this.status != ContactStatus.PENDING) {
            throw new ContactStatusViolationException();
        }
        if (!this.requesterId.equals(actorId)) {
            throw new ContactNotFoundException();
        }
    }

    public void blockUser(UserId actorId) {
        if (this.status == ContactStatus.BLOCKED) {
            return;
        }
        this.status = ContactStatus.BLOCKED;
        // Có thể bổ sung Domain Event ContactBlockedEvent nếu cần các module khác xử lý theo (ví dụ: hủy kết nối chat realtime)
    }

    public void unblockUser(UserId actorId) {
        if (this.status != ContactStatus.BLOCKED) {
            return;
        }
    }

    //public void changeNickname(UserId actorId, String nickname) {
    //     if (this.status != ContactStatus.ACCEPTED) {
    //         throw new IllegalStateException("Chỉ có thể đặt bí danh sau khi đã kết bạn");
    //     }
    //     if (actorId.equals(this.requesterId)) {
    //         this.nicknameByRequester = nickname;
    //     } else if (actorId.equals(this.addresseeId)) {
    //         this.nicknameByAddressee = nickname;
    //     } else {
    //         throw new IllegalArgumentException("Người thực thi không thuộc liên hệ này");
    //     }
    // }

    // 7. Tính năng bổ sung: Hạn chế (Mute / Restrict)
    // public void toggleRestriction(UserId actorId, boolean restrict) {
    //     if (actorId.equals(this.requesterId)) {
    //         this.isRestrictedByRequester = restrict;
    //     } else if (actorId.equals(this.addresseeId)) {
    //         this.isRestrictedByAddressee = restrict;
    //     } else {
    //         throw new IllegalArgumentException("Người thực thi không thuộc liên hệ này");
    //     }
    // }

    // Getters sạch (Chỉ cung cấp Unmodifiable List cho Events để bảo vệ tính đóng gói)
    public UUID getId() { return id; }
    public UserId getRequesterId() { return requesterId; }
    public UserId getAddresseeId() { return addresseeId; }
    public ContactStatus getStatus() { return status; }
    //public String getNicknameByRequester() { return nicknameByRequester; }
    //public String getNicknameByAddressee() { return nicknameByAddressee; }
    //public boolean isRestrictedByRequester() { return isRestrictedByRequester; }
    //public boolean isRestrictedByAddressee() { return isRestrictedByAddressee; }
}